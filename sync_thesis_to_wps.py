from __future__ import annotations

import argparse
import html
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import List

import pythoncom
import win32com.client as win32
from PIL import Image, ImageChops


HEADING_RE = re.compile(r"^(#{2,6})\s+(.*)$")
FENCE_RE = re.compile(r"^```([A-Za-z0-9_-]+)?\s*$")
TABLE_SEP_RE = re.compile(r"^\|\s*[:\-| ]+\|\s*$")
FIG_REF_RE = re.compile(r"^如图\s+(\d+-\d+)\s+(.+?)所示。?$")
TABLE_CAPTION_RE = re.compile(r"^表\s+\d+-\d+\s+.+$")
FIG_CAPTION_RE = re.compile(r"^图\s+\d+-\d+\s+.+$")
BOLD_LINE_RE = re.compile(r"^\*\*(.+?)\*\*$")


@dataclass
class Block:
    type: str
    text: str = ""
    level: int = 0
    rows: List[List[str]] | None = None
    lang: str = ""
    caption: str | None = None
    image_path: str | None = None
    bold: bool = False


def clean_inline(text: str) -> str:
    text = text.rstrip()
    if text.endswith("\\"):
        text = text[:-1].rstrip()
    text = re.sub(r"`([^`]+)`", r"\1", text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"\1", text)
    text = re.sub(r"__([^_]+)__", r"\1", text)
    return text.strip()


def normalize_paragraph(lines: List[str]) -> str:
    cleaned = [clean_inline(line.strip()) for line in lines if line.strip()]
    if not cleaned:
        return ""
    return " ".join(cleaned).strip()


def parse_table(lines: List[str], start: int) -> tuple[Block, int]:
    rows: List[List[str]] = []
    i = start
    while i < len(lines) and lines[i].strip().startswith("|"):
        line = lines[i].strip()
        if i == start + 1 and TABLE_SEP_RE.match(line):
            i += 1
            continue
        parts = [clean_inline(cell.strip()) for cell in line.strip("|").split("|")]
        rows.append(parts)
        i += 1
    return Block(type="table", rows=rows), i


def parse_markdown(md_text: str) -> List[Block]:
    lines = md_text.splitlines()
    start = 0
    for idx, line in enumerate(lines):
        if line.strip() == "## 摘要":
            start = idx
            break
    lines = lines[start:]

    blocks: List[Block] = []
    paragraph_buffer: List[str] = []
    i = 0

    def flush_paragraph() -> None:
        nonlocal paragraph_buffer
        text = normalize_paragraph(paragraph_buffer)
        if text:
            bold_match = BOLD_LINE_RE.match(text)
            if bold_match:
                blocks.append(Block(type="paragraph", text=clean_inline(bold_match.group(1)), bold=True))
            else:
                blocks.append(Block(type="paragraph", text=text))
        paragraph_buffer = []

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        heading_match = HEADING_RE.match(stripped)
        fence_match = FENCE_RE.match(stripped)

        if not stripped:
            flush_paragraph()
            i += 1
            continue

        if heading_match:
            flush_paragraph()
            level = len(heading_match.group(1)) - 1
            blocks.append(Block(type="heading", level=level, text=clean_inline(heading_match.group(2))))
            i += 1
            continue

        if stripped.startswith("|") and i + 1 < len(lines) and TABLE_SEP_RE.match(lines[i + 1].strip()):
            flush_paragraph()
            table_block, i = parse_table(lines, i)
            blocks.append(table_block)
            continue

        if fence_match:
            flush_paragraph()
            lang = (fence_match.group(1) or "").lower()
            i += 1
            code_lines: List[str] = []
            while i < len(lines) and lines[i].strip() != "```":
                code_lines.append(lines[i].rstrip("\n"))
                i += 1
            i += 1
            code_text = "\n".join(code_lines).strip("\n")
            block_type = "figure" if lang == "mermaid" else "code"
            caption = None
            if block_type == "figure":
                for prev in reversed(blocks):
                    if prev.type == "paragraph" and prev.text:
                        match = FIG_REF_RE.match(prev.text)
                        if match:
                            caption = f"图 {match.group(1)} {match.group(2)}"
                        break
            blocks.append(Block(type=block_type, text=code_text, lang=lang, caption=caption))
            continue

        paragraph_buffer.append(line)
        i += 1

    flush_paragraph()
    return blocks


def crop_white_space(image_path: Path) -> None:
    image = Image.open(image_path).convert("RGB")
    background = Image.new("RGB", image.size, "white")
    diff = ImageChops.difference(image, background)
    bbox = diff.getbbox()
    if not bbox:
        return
    pad = 24
    left = max(0, bbox[0] - pad)
    top = max(0, bbox[1] - pad)
    right = min(image.width, bbox[2] + pad)
    bottom = min(image.height, bbox[3] + pad)
    image.crop((left, top, right, bottom)).save(image_path)


def render_mermaid_block(diagram_text: str, output_path: Path, edge_path: Path) -> Path:
    html_path = output_path.with_suffix(".html")
    escaped = html.escape(diagram_text)
    html_content = f"""<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <style>
    body {{
      margin: 0;
      padding: 24px;
      background: white;
      width: fit-content;
      height: fit-content;
    }}
    .mermaid {{
      font-family: "Microsoft YaHei", "Segoe UI", sans-serif;
    }}
  </style>
  <script type="module">
    import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
    mermaid.initialize({{
      startOnLoad: true,
      theme: 'default',
      securityLevel: 'loose'
    }});
  </script>
</head>
<body>
  <pre class="mermaid">{escaped}</pre>
</body>
</html>
"""
    html_path.write_text(html_content, encoding="utf-8")
    subprocess.run(
        [
            str(edge_path),
            "--headless",
            "--disable-gpu",
            "--hide-scrollbars",
            "--window-size=2800,2200",
            "--virtual-time-budget=10000",
            f"--screenshot={output_path}",
            str(html_path),
        ],
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    crop_white_space(output_path)
    return output_path


def prepare_figure_assets(blocks: List[Block], work_dir: Path, edge_path: Path) -> None:
    figure_dir = work_dir / "mermaid_figures"
    figure_dir.mkdir(parents=True, exist_ok=True)
    counter = 1
    for block in blocks:
        if block.type != "figure":
            continue
        image_path = figure_dir / f"figure_{counter:02d}.png"
        try:
            render_mermaid_block(block.text, image_path, edge_path)
            block.image_path = str(image_path)
        except Exception as exc:  # noqa: BLE001
            print(f"[warn] Mermaid 渲染失败，改为源码块插入：{exc}")
            block.type = "code"
            block.lang = "mermaid"
        counter += 1


def style_exists(doc, style_name: str) -> bool:
    try:
        _ = doc.Styles(style_name)
        return True
    except Exception:  # noqa: BLE001
        return False


def get_body_start(doc) -> int:
    for i in range(1, doc.Paragraphs.Count + 1):
        text = doc.Paragraphs(i).Range.Text.replace("\r", "").replace("\x07", "").strip()
        if text == "摘要":
            return doc.Paragraphs(i).Range.Start
    raise RuntimeError("未在目标文档中定位到正文“摘要”标题，已停止写入。")


class WordWriter:
    def __init__(self, doc):
        self.doc = doc
        self.app = doc.Application
        self.sel = self.app.Selection
        self.heading_styles = {
            1: "标题 1",
            2: "标题 2",
            3: "标题 3",
            4: "标题 4",
        }
        self.body_style = "正文" if style_exists(doc, "正文") else "Normal"
        self.first_heading_written = False

    def _apply_style(self, style_name: str) -> None:
        try:
            self.sel.Style = style_name
        except Exception:  # noqa: BLE001
            pass

    def _paragraph_range(self, start: int):
        return self.doc.Range(start, self.sel.Range.Start)

    def insert_page_break(self) -> None:
        self.sel.InsertBreak(7)

    def write_heading(self, text: str, level: int) -> None:
        if self.first_heading_written and level == 1:
            self.insert_page_break()
        style_name = self.heading_styles.get(level, "标题 4")
        self._apply_style(style_name)
        start = self.sel.Range.Start
        self.sel.TypeText(text)
        rng = self._paragraph_range(start)
        rng.ParagraphFormat.Alignment = 0
        self.sel.TypeParagraph()
        self.first_heading_written = True

    def write_paragraph(self, text: str, *, bold: bool = False, center: bool = False) -> None:
        self._apply_style(self.body_style)
        start = self.sel.Range.Start
        self.sel.TypeText(text)
        rng = self._paragraph_range(start)
        rng.ParagraphFormat.Alignment = 1 if center else 3
        rng.Font.Bold = -1 if bold else 0
        self.sel.TypeParagraph()

    def write_code(self, text: str) -> None:
        self._apply_style(self.body_style)
        start = self.sel.Range.Start
        self.sel.TypeText(text.replace("\n", "\v"))
        rng = self._paragraph_range(start)
        rng.ParagraphFormat.Alignment = 0
        rng.ParagraphFormat.LeftIndent = self.app.CentimetersToPoints(0.74)
        rng.ParagraphFormat.SpaceAfter = 0
        rng.Font.Name = "Consolas"
        rng.Font.Size = 9
        self.sel.TypeParagraph()
        self.sel.TypeParagraph()

    def write_table(self, rows: List[List[str]]) -> None:
        if not rows:
            return
        col_count = max(len(row) for row in rows)
        normalized = [row + [""] * (col_count - len(row)) for row in rows]
        start = self.sel.Range.Start
        table_text = "\r".join("\t".join(row) for row in normalized)
        self.sel.TypeText(table_text)
        rng = self.doc.Range(start, self.sel.Range.Start)
        table = rng.ConvertToTable(
            Separator=1,
            NumColumns=col_count,
            AutoFitBehavior=2,
            ApplyBorders=True,
        )
        table.Range.Font.Name = "宋体"
        table.Range.Font.Size = 10.5
        for r_idx, row in enumerate(normalized, start=1):
            for c_idx, cell_text in enumerate(row, start=1):
                cell_range = table.Cell(r_idx, c_idx).Range
                cell_range.Text = cell_text
                cell_range.ParagraphFormat.Alignment = 1
        try:
            table.Rows(1).Range.Bold = True
        except Exception:  # noqa: BLE001
            pass
        try:
            table.AutoFitBehavior(2)
        except Exception:  # noqa: BLE001
            pass
        self.sel.SetRange(table.Range.End, table.Range.End)
        self.sel.TypeParagraph()

    def write_figure(self, image_path: str, caption: str | None = None) -> None:
        self._apply_style(self.body_style)
        self.sel.ParagraphFormat.Alignment = 1
        shape = self.sel.InlineShapes.AddPicture(image_path, False, True)
        max_width = self.app.CentimetersToPoints(15.5)
        if shape.Width > max_width:
            ratio = max_width / shape.Width
            shape.Width = max_width
            shape.Height = shape.Height * ratio
        self.sel.TypeParagraph()
        if caption:
            self.write_paragraph(caption, center=True)
        else:
            self.sel.TypeParagraph()
        self.sel.ParagraphFormat.Alignment = 3


def update_document(blocks: List[Block], doc_path: Path, backup: bool) -> Path:
    if backup:
        backup_path = doc_path.with_name(f"{doc_path.stem}.backup{doc_path.suffix}")
        shutil.copy2(doc_path, backup_path)
        print(f"[info] 已创建备份：{backup_path}")

    pythoncom.CoInitialize()
    try:
        doc = win32.GetObject(str(doc_path))
        app = doc.Application
        app.ScreenUpdating = False
        start_pos = get_body_start(doc)
        delete_range = doc.Range(start_pos, doc.Content.End - 1)
        delete_range.Delete()

        writer = WordWriter(doc)
        writer.sel.SetRange(start_pos, start_pos)

        for block in blocks:
            if block.type == "heading":
                writer.write_heading(block.text, block.level)
            elif block.type == "paragraph":
                center = bool(TABLE_CAPTION_RE.match(block.text) or FIG_CAPTION_RE.match(block.text))
                writer.write_paragraph(block.text, bold=block.bold, center=center)
            elif block.type == "table":
                writer.write_table(block.rows or [])
            elif block.type == "code":
                writer.write_code(block.text)
            elif block.type == "figure":
                if block.image_path:
                    writer.write_figure(block.image_path, block.caption)
                else:
                    writer.write_code(block.text)

        try:
            doc.Fields.Update()
        except Exception:  # noqa: BLE001
            pass
        try:
            for i in range(1, doc.TablesOfContents.Count + 1):
                doc.TablesOfContents(i).Update()
        except Exception:  # noqa: BLE001
            pass
        doc.Save()
        app.ScreenUpdating = True
    finally:
        pythoncom.CoUninitialize()

    return doc_path


def find_edge_path() -> Path:
    candidates = [
        Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"),
        Path(r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"),
        Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe"),
        Path(r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    raise FileNotFoundError("未找到可用的 Edge/Chrome 浏览器，无法本地渲染 Mermaid 图。")


def main() -> int:
    parser = argparse.ArgumentParser(description="将 Markdown 论文内容同步到当前已打开的 WPS 文档。")
    parser.add_argument("--markdown", required=True, help="Markdown 源文件路径")
    parser.add_argument("--document", required=True, help="已在 WPS 中打开的 Word 文档路径")
    parser.add_argument("--work-dir", default=str(Path.cwd() / "_wps_sync_tmp"), help="临时资源目录")
    parser.add_argument("--no-backup", action="store_true", help="不创建 docx 备份")
    args = parser.parse_args()

    md_path = Path(args.markdown)
    doc_path = Path(args.document)
    work_dir = Path(args.work_dir)
    work_dir.mkdir(parents=True, exist_ok=True)

    if not md_path.exists():
        raise FileNotFoundError(f"未找到 Markdown 文件：{md_path}")
    if not doc_path.exists():
        raise FileNotFoundError(f"未找到 Word 文档：{doc_path}")

    md_text = md_path.read_text(encoding="utf-8")
    blocks = parse_markdown(md_text)
    edge_path = find_edge_path()
    prepare_figure_assets(blocks, work_dir, edge_path)
    update_document(blocks, doc_path, backup=not args.no_backup)
    print(f"[done] 已同步到文档：{doc_path}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:  # noqa: BLE001
        print(f"[error] {exc}")
        raise
