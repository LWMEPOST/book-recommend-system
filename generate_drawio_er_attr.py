from __future__ import annotations

from pathlib import Path
from xml.etree.ElementTree import Element, SubElement, ElementTree


class IdGen:
    def __init__(self) -> None:
        self.value = 2

    def next(self) -> str:
        current = str(self.value)
        self.value += 1
        return current


def mx_cell(root, cell_id: str, value: str = "", style: str = "", vertex: bool = False,
            edge: bool = False, parent: str = "1", x: float | None = None, y: float | None = None,
            w: float | None = None, h: float | None = None, source: str | None = None,
            target: str | None = None, relative: bool = False):
    attrs = {"id": cell_id, "value": value, "parent": parent}
    if style:
        attrs["style"] = style
    if vertex:
        attrs["vertex"] = "1"
    if edge:
        attrs["edge"] = "1"
    if source:
        attrs["source"] = source
    if target:
        attrs["target"] = target
    cell = SubElement(root, "mxCell", attrs)
    geo_attrs = {"as": "geometry"}
    if edge:
        geo_attrs["relative"] = "1" if relative else "0"
        geo = SubElement(cell, "mxGeometry", geo_attrs)
        if source and target:
            geo.set("relative", "1")
    else:
        if x is not None:
            geo_attrs["x"] = str(x)
        if y is not None:
            geo_attrs["y"] = str(y)
        if w is not None:
            geo_attrs["width"] = str(w)
        if h is not None:
            geo_attrs["height"] = str(h)
        SubElement(cell, "mxGeometry", geo_attrs)
    return cell


def build_page(page_name: str, width: int, height: int):
    diagram = Element("diagram", {"id": page_name, "name": page_name})
    model = SubElement(
        diagram,
        "mxGraphModel",
        {
            "dx": "1480",
            "dy": "860",
            "grid": "1",
            "gridSize": "10",
            "guides": "1",
            "tooltips": "1",
            "connect": "1",
            "arrows": "1",
            "fold": "1",
            "page": "1",
            "pageScale": "1",
            "pageWidth": str(width),
            "pageHeight": str(height),
            "math": "0",
            "shadow": "0",
        },
    )
    root = SubElement(model, "root")
    SubElement(root, "mxCell", {"id": "0"})
    SubElement(root, "mxCell", {"id": "1", "parent": "0"})
    return diagram, root


ENTITY_STYLE = (
    "rounded=0;whiteSpace=wrap;html=1;fillColor=#DDE8FF;strokeColor=#8CA4D8;"
    "fontColor=#333333;fontStyle=1;fontSize=13;"
)
REL_STYLE = (
    "shape=rhombus;whiteSpace=wrap;html=1;fillColor=#FFE9C9;strokeColor=#D6A566;"
    "fontColor=#333333;fontSize=12;"
)
ATTR_STYLE = (
    "ellipse;whiteSpace=wrap;html=1;fillColor=#EEE6FF;strokeColor=#B7A6D9;"
    "fontColor=#333333;fontSize=11;"
)
ATTR_PK_STYLE = (
    "ellipse;whiteSpace=wrap;html=1;fillColor=#EEE6FF;strokeColor=#B7A6D9;"
    "fontColor=#333333;fontSize=11;fontStyle=4;"
)
EDGE_STYLE = (
    "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;"
    "html=1;strokeColor=#333333;endArrow=none;startArrow=none;strokeWidth=1.5;"
    "align=center;verticalAlign=middle;labelBackgroundColor=#FFFFFF;"
)
ATTR_EDGE_STYLE = (
    "edgeStyle=none;rounded=0;orthogonalLoop=1;jettySize=auto;"
    "html=1;strokeColor=#333333;endArrow=none;startArrow=none;strokeWidth=1;"
)
NOTE_STYLE = (
    "rounded=1;whiteSpace=wrap;html=1;fillColor=#F7FAFF;strokeColor=#9DBCEB;"
    "fontColor=#333333;fontSize=11;"
)


def add_entity(root, ids: IdGen, label: str, x: float, y: float, w: float = 150, h: float = 56) -> str:
    cell_id = ids.next()
    mx_cell(root, cell_id, label, ENTITY_STYLE, vertex=True, x=x, y=y, w=w, h=h)
    return cell_id


def add_relation(root, ids: IdGen, label: str, x: float, y: float, w: float = 96, h: float = 62) -> str:
    cell_id = ids.next()
    mx_cell(root, cell_id, label, REL_STYLE, vertex=True, x=x, y=y, w=w, h=h)
    return cell_id


def add_attribute(root, ids: IdGen, label: str, x: float, y: float, pk: bool = False,
                  w: float = 100, h: float = 38) -> str:
    cell_id = ids.next()
    mx_cell(root, cell_id, label, ATTR_PK_STYLE if pk else ATTR_STYLE, vertex=True, x=x, y=y, w=w, h=h)
    return cell_id


def add_edge(root, ids: IdGen, source: str, target: str, label: str = "", attr: bool = False):
    mx_cell(root, ids.next(), label, ATTR_EDGE_STYLE if attr else EDGE_STYLE,
            edge=True, source=source, target=target, relative=True)


def add_entity_with_attrs(root, ids: IdGen, entity_label: str, x: float, y: float, attrs: list[tuple[str, float, float, bool]],
                          ew: float = 150, eh: float = 52):
    entity_id = add_entity(root, ids, entity_label, x, y, ew, eh)
    center_x = x + ew / 2
    center_y = y + eh / 2
    for label, dx, dy, pk in attrs:
        attr_w = 104 if len(label) <= 8 else 118
        attr_h = 34
        ax = center_x + dx - attr_w / 2
        ay = center_y + dy - attr_h / 2
        attr_id = add_attribute(root, ids, label, ax, ay, pk=pk, w=attr_w, h=attr_h)
        add_edge(root, ids, entity_id, attr_id, attr=True)
    return entity_id


ATTR_ENTITY_PAGES = [
    (
        "用户实体属性图",
        "用户",
        [
            ("编号", True),
            ("用户名", False),
            ("密码", False),
            ("昵称", False),
            ("邮箱", False),
            ("手机号", False),
            ("头像", False),
            ("角色", False),
            ("状态", False),
        ],
    ),
    (
        "分类实体属性图",
        "分类",
        [
            ("编号", True),
            ("分类名称", False),
            ("排序号", False),
            ("创建时间", False),
        ],
    ),
    (
        "图书实体属性图",
        "图书",
        [
            ("编号", True),
            ("书名", False),
            ("作者", False),
            ("ISBN", False),
            ("出版社", False),
            ("出版日期", False),
            ("分类编号", False),
            ("馆藏总数", False),
            ("可借数量", False),
            ("封面地址", False),
            ("图书简介", False),
            ("平均评分", False),
            ("评分人数", False),
            ("状态", False),
        ],
    ),
    (
        "借阅记录实体属性图",
        "借阅记录",
        [
            ("编号", True),
            ("用户编号", False),
            ("图书编号", False),
            ("借阅时间", False),
            ("应还时间", False),
            ("归还时间", False),
            ("状态", False),
        ],
    ),
    (
        "逾期记录实体属性图",
        "逾期记录",
        [
            ("编号", True),
            ("借阅编号", False),
            ("用户编号", False),
            ("图书编号", False),
            ("逾期天数", False),
            ("罚款金额", False),
            ("状态", False),
        ],
    ),
    (
        "收藏记录实体属性图",
        "收藏记录",
        [
            ("编号", True),
            ("用户编号", False),
            ("图书编号", False),
            ("收藏时间", False),
        ],
    ),
    (
        "评分记录实体属性图",
        "评分记录",
        [
            ("编号", True),
            ("用户编号", False),
            ("图书编号", False),
            ("评分值", False),
            ("创建时间", False),
            ("更新时间", False),
        ],
    ),
    (
        "公告实体属性图",
        "公告",
        [
            ("编号", True),
            ("标题", False),
            ("内容", False),
            ("管理员编号", False),
            ("状态", False),
            ("创建时间", False),
        ],
    ),
    (
        "算法配置实体属性图",
        "算法配置",
        [
            ("编号", True),
            ("配置键", False),
            ("配置值", False),
            ("配置说明", False),
            ("更新时间", False),
        ],
    ),
]


def get_attr_slots(count: int) -> list[tuple[int, int]]:
    # 按边缘到边缘约 80px 的距离组织属性点位
    slots = [
        (0, -125),
        (150, -105),
        (225, -20),
        (225, 85),
        (150, 170),
        (0, 195),
        (-150, 170),
        (-225, 85),
        (-225, -20),
        (-150, -105),
        (0, -210),
        (0, 280),
        (300, 30),
        (-300, 30),
    ]
    if count > len(slots):
        raise ValueError(f"属性数量过多，当前槽位不足: {count}")
    return slots[:count]


def add_attr_entity_page(mxfile_root: Element, page_name: str, entity_label: str,
                         attrs: list[tuple[str, bool]]):
    diagram, root = build_page(page_name, 1200, 900)
    ids = IdGen()

    entity_x = 500
    entity_y = 400
    entity_w = 180
    entity_h = 64
    entity_center_x = entity_x + entity_w / 2
    entity_center_y = entity_y + entity_h / 2

    entity_id = add_entity(root, ids, entity_label, entity_x, entity_y, entity_w, entity_h)

    for (label, pk), (dx, dy) in zip(attrs, get_attr_slots(len(attrs)), strict=False):
        attr_w = 96 if len(label) <= 4 else 116
        attr_h = 36
        attr_x = entity_center_x + dx - attr_w / 2
        attr_y = entity_center_y + dy - attr_h / 2
        attr_id = add_attribute(root, ids, label, attr_x, attr_y, pk=pk, w=attr_w, h=attr_h)
        add_edge(root, ids, entity_id, attr_id, attr=True)

    mxfile_root.append(diagram)


def add_er_page(mxfile_root: Element):
    diagram, root = build_page("图4-10 Chen风格ER图", 2000, 1320)
    ids = IdGen()

    # Main horizontal chain: 用户 -> 借阅记录 -> 图书 -> 分类
    user = add_entity(root, ids, "用户", 80, 520, 160, 58)
    rel_borrow = add_relation(root, ids, "借阅", 300, 518, 96, 66)
    borrow_record = add_entity(root, ids, "借阅记录", 470, 515, 210, 64)
    rel_ref_book = add_relation(root, ids, "对应图书", 760, 518, 104, 66)
    book = add_entity(root, ids, "图书", 940, 520, 160, 58)
    rel_belong = add_relation(root, ids, "属于", 1165, 518, 88, 66)
    category = add_entity(root, ids, "分类", 1325, 520, 170, 58)

    add_edge(root, ids, user, rel_borrow, "1")
    add_edge(root, ids, rel_borrow, borrow_record, "N")
    add_edge(root, ids, borrow_record, rel_ref_book, "N")
    add_edge(root, ids, rel_ref_book, book, "1")
    add_edge(root, ids, book, rel_belong, "N")
    add_edge(root, ids, rel_belong, category, "1")

    # Upper-left branch: publish notice
    notice = add_entity(root, ids, "公告", 90, 180, 170, 58)
    rel_publish = add_relation(root, ids, "发布", 265, 340, 92, 62)
    add_edge(root, ids, user, rel_publish, "1")
    add_edge(root, ids, rel_publish, notice, "N")

    # Upper-right branch: favorite
    favorite = add_entity(root, ids, "收藏记录", 1220, 180, 180, 58)
    rel_fav_user = add_relation(root, ids, "收藏", 360, 220, 92, 62)
    rel_fav_book = add_relation(root, ids, "收藏对象", 1080, 320, 108, 62)
    add_edge(root, ids, user, rel_fav_user, "1")
    add_edge(root, ids, rel_fav_user, favorite, "N")
    add_edge(root, ids, book, rel_fav_book, "1")
    add_edge(root, ids, rel_fav_book, favorite, "N")

    # Lower-right branch: rating
    rating = add_entity(root, ids, "评分记录", 1220, 860, 180, 58)
    rel_rate_user = add_relation(root, ids, "评分", 360, 820, 92, 62)
    rel_rate_book = add_relation(root, ids, "评分对象", 1080, 760, 108, 62)
    add_edge(root, ids, user, rel_rate_user, "1")
    add_edge(root, ids, rel_rate_user, rating, "N")
    add_edge(root, ids, book, rel_rate_book, "1")
    add_edge(root, ids, rel_rate_book, rating, "N")

    # Bottom center branch: overdue record
    overdue = add_entity(root, ids, "逾期记录", 590, 1030, 220, 64)
    rel_generate = add_relation(root, ids, "生成", 540, 865, 92, 62)
    rel_owner = add_relation(root, ids, "归属用户", 300, 965, 108, 62)
    rel_over_book = add_relation(root, ids, "涉及图书", 905, 965, 108, 62)
    add_edge(root, ids, borrow_record, rel_generate, "1")
    add_edge(root, ids, rel_generate, overdue, "N")
    add_edge(root, ids, user, rel_owner, "1")
    add_edge(root, ids, rel_owner, overdue, "N")
    add_edge(root, ids, book, rel_over_book, "1")
    add_edge(root, ids, rel_over_book, overdue, "N")

    # Independent config entity, placed top-right like side note
    algo = add_entity(root, ids, "算法配置", 1630, 220, 230, 64)
    note = ids.next()
    mx_cell(
        root,
        note,
        "独立配置实体\\n用于维护推荐算法参数，\\n在数据库层无外键依赖。",
        NOTE_STYLE,
        vertex=True,
        x=1615,
        y=310,
        w=250,
        h=84,
    )

    mxfile_root.append(diagram)


def add_attr_pages(mxfile_root: Element):
    for page_name, entity_label, attrs in ATTR_ENTITY_PAGES:
        add_attr_entity_page(mxfile_root, page_name, entity_label, attrs)


def build_single_page_file(page_builder, out_path: Path):
    mxfile = Element(
        "mxfile",
        {
            "host": "app.diagrams.net",
            "agent": "Codex",
            "version": "24.7.17",
            "type": "device",
            "compressed": "false",
        },
    )
    page_builder(mxfile)
    ElementTree(mxfile).write(out_path, encoding="utf-8", xml_declaration=True)


def build_multi_page_file(out_path: Path):
    mxfile = Element(
        "mxfile",
        {
            "host": "app.diagrams.net",
            "agent": "Codex",
            "version": "24.7.17",
            "type": "device",
            "compressed": "false",
        },
    )
    add_attr_pages(mxfile)
    add_er_page(mxfile)
    ElementTree(mxfile).write(out_path, encoding="utf-8", xml_declaration=True)


def build_attr_multi_page_file(out_path: Path):
    mxfile = Element(
        "mxfile",
        {
            "host": "app.diagrams.net",
            "agent": "Codex",
            "version": "24.7.17",
            "type": "device",
            "compressed": "false",
        },
    )
    add_attr_pages(mxfile)
    ElementTree(mxfile).write(out_path, encoding="utf-8", xml_declaration=True)


def main():
    out_dir = Path(r"D:\XM\XSJ\drawio")
    out_dir.mkdir(parents=True, exist_ok=True)

    build_multi_page_file(out_dir / "图3-2_图4-10_实体属性图与ChenER.drawio")
    build_attr_multi_page_file(out_dir / "图3-2_实体属性图.drawio")
    build_single_page_file(add_er_page, out_dir / "图4-10_Chen风格ER图.drawio")

    print("drawio files generated:")
    for path in sorted(out_dir.glob("*.drawio")):
        print(path)


if __name__ == "__main__":
    main()
