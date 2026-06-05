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


def build_mxgraph(page_name: str, width: int, height: int):
    diagram = Element("diagram", {"id": page_name, "name": page_name})
    model = SubElement(
        diagram,
        "mxGraphModel",
        {
            "dx": "1280",
            "dy": "720",
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


def add_architecture_page(diagram_file_root: Element):
    diagram, root = build_mxgraph("图4-1 系统总体架构图", 1400, 960)
    ids = IdGen()

    layer_style = (
        "rounded=1;whiteSpace=wrap;html=1;fillColor=#F5F5F5;strokeColor=#333333;"
        "fontColor=#333333;fontStyle=1;fontSize=13;align=left;verticalAlign=top;spacing=12;"
    )
    comp_style = (
        "rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#333333;"
        "fontColor=#333333;fontSize=12;"
    )
    emph_style = (
        "rounded=1;whiteSpace=wrap;html=1;fillColor=#FFF2E8;strokeColor=#C97A45;"
        "fontColor=#333333;fontSize=12;"
    )
    db_style = (
        "shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;"
        "fillColor=#FFFFFF;strokeColor=#333333;fontColor=#333333;fontSize=12;"
    )
    edge_style = (
        "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;"
        "html=1;strokeColor=#333333;endArrow=blockThin;endFill=1;"
    )
    dashed_edge_style = (
        "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;"
        "strokeColor=#999999;endArrow=classic;endFill=1;dashed=1;"
    )

    # Layer backgrounds
    mx_cell(root, ids.next(), "表现层", layer_style, vertex=True, x=40, y=30, w=1320, h=120)
    mx_cell(root, ids.next(), "接口与安全层", layer_style, vertex=True, x=40, y=170, w=1320, h=120)
    mx_cell(root, ids.next(), "业务逻辑层", layer_style, vertex=True, x=40, y=310, w=1320, h=170)
    mx_cell(root, ids.next(), "数据持久层", layer_style, vertex=True, x=40, y=500, w=1320, h=100)
    mx_cell(root, ids.next(), "存储资源层", layer_style, vertex=True, x=40, y=620, w=1320, h=150)

    # Components
    a = ids.next()
    mx_cell(root, a, "用户端页面 / 管理端页面", comp_style, vertex=True, x=120, y=75, w=260, h=48)
    b = ids.next()
    mx_cell(root, b, "Vue 3 + Element Plus", comp_style, vertex=True, x=470, y=75, w=240, h=48)
    c = ids.next()
    mx_cell(root, c, "Axios 请求与路由守卫", comp_style, vertex=True, x=800, y=75, w=250, h=48)

    d = ids.next()
    mx_cell(root, d, "Spring Security + JWT 过滤器", comp_style, vertex=True, x=270, y=215, w=300, h=48)
    e = ids.next()
    mx_cell(root, e, "Controller 控制层", comp_style, vertex=True, x=760, y=215, w=220, h=48)

    f = ids.next()
    mx_cell(root, f, "Service 业务层", comp_style, vertex=True, x=530, y=350, w=220, h=52)
    i = ids.next()
    mx_cell(root, i, "推荐计算与缓存", emph_style, vertex=True, x=210, y=410, w=250, h=50)
    j = ids.next()
    mx_cell(root, j, "封面上传与静态资源访问", emph_style, vertex=True, x=860, y=410, w=270, h=50)

    g = ids.next()
    mx_cell(root, g, "MyBatis-Plus / Mapper", comp_style, vertex=True, x=500, y=525, w=280, h=50)

    h = ids.next()
    mx_cell(root, h, "MySQL 数据库", db_style, vertex=True, x=280, y=670, w=240, h=70)
    k = ids.next()
    mx_cell(root, k, "本地 uploads 目录", db_style, vertex=True, x=840, y=670, w=260, h=70)

    # Edges
    for source, target in [
        (a, b),
        (b, c),
        (c, d),
        (d, e),
        (e, f),
        (f, g),
        (g, h),
        (f, i),
        (f, j),
        (j, k),
    ]:
        mx_cell(root, ids.next(), "", edge_style, edge=True, source=source, target=target, relative=True)

    mx_cell(root, ids.next(), "推荐结果回传", dashed_edge_style, edge=True, source=i, target=e, relative=True)

    diagram_file_root.append(diagram)


def add_module_page(diagram_file_root: Element):
    diagram, root = build_mxgraph("图4-2 系统功能模块图", 1400, 980)
    ids = IdGen()

    root_style = (
        "rounded=1;whiteSpace=wrap;html=1;fillColor=#3366CC;strokeColor=#264A99;"
        "fontColor=#FFFFFF;fontStyle=1;fontSize=14;"
    )
    branch_style = (
        "rounded=1;whiteSpace=wrap;html=1;fillColor=#7FBFFF;strokeColor=#3366CC;"
        "fontColor=#333333;fontStyle=1;fontSize=13;"
    )
    node_style = (
        "rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#3366CC;"
        "fontColor=#333333;fontSize=12;"
    )
    group_style = (
        "rounded=1;whiteSpace=wrap;html=1;fillColor=#F7FAFF;strokeColor=#9DBCEB;"
        "fontColor=#333333;fontSize=12;align=left;verticalAlign=top;spacing=10;"
    )
    edge_style = (
        "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;"
        "html=1;strokeColor=#3366CC;endArrow=blockThin;endFill=1;"
    )

    # Background groups
    mx_cell(root, ids.next(), "用户端功能域", group_style, vertex=True, x=60, y=150, w=580, h=720)
    mx_cell(root, ids.next(), "管理端功能域", group_style, vertex=True, x=760, y=150, w=580, h=720)

    root_id = ids.next()
    mx_cell(root, root_id, "图书推荐系统", root_style, vertex=True, x=540, y=40, w=320, h=64)
    user_id = ids.next()
    mx_cell(root, user_id, "用户端模块", branch_style, vertex=True, x=240, y=190, w=220, h=56)
    admin_id = ids.next()
    mx_cell(root, admin_id, "管理端模块", branch_style, vertex=True, x=940, y=190, w=220, h=56)

    user_nodes = [
        ("注册登录", 110, 310),
        ("图书浏览与检索", 320, 310),
        ("图书详情", 110, 430),
        ("借阅与归还", 320, 430),
        ("收藏与评分", 110, 550),
        ("推荐中心", 320, 550),
        ("借阅记录与逾期记录", 110, 670),
        ("系统公告", 320, 670),
    ]
    admin_nodes = [
        ("图书管理", 810, 310),
        ("封面上传", 1020, 310),
        ("批量导入", 1230, 310),
        ("用户管理", 810, 470),
        ("借阅管理", 1020, 470),
        ("逾期管理", 1230, 470),
        ("公告管理", 915, 630),
        ("算法管理", 1125, 630),
    ]

    user_cell_ids = []
    for label, x, y in user_nodes:
        node_id = ids.next()
        mx_cell(root, node_id, label, node_style, vertex=True, x=x, y=y, w=170, h=52)
        user_cell_ids.append(node_id)

    admin_cell_ids = []
    for label, x, y in admin_nodes:
        node_id = ids.next()
        mx_cell(root, node_id, label, node_style, vertex=True, x=x, y=y, w=170, h=52)
        admin_cell_ids.append(node_id)

    mx_cell(root, ids.next(), "", edge_style, edge=True, source=root_id, target=user_id, relative=True)
    mx_cell(root, ids.next(), "", edge_style, edge=True, source=root_id, target=admin_id, relative=True)

    for node_id in user_cell_ids:
        mx_cell(root, ids.next(), "", edge_style, edge=True, source=user_id, target=node_id, relative=True)

    for node_id in admin_cell_ids:
        mx_cell(root, ids.next(), "", edge_style, edge=True, source=admin_id, target=node_id, relative=True)

    diagram_file_root.append(diagram)


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
    add_architecture_page(mxfile)
    add_module_page(mxfile)
    ElementTree(mxfile).write(out_path, encoding="utf-8", xml_declaration=True)


def main():
    out_dir = Path(r"D:\XM\XSJ\drawio")
    out_dir.mkdir(parents=True, exist_ok=True)

    build_multi_page_file(out_dir / "图4-1_图4-2_系统架构与功能模块.drawio")
    build_single_page_file(add_architecture_page, out_dir / "图4-1_系统总体架构图.drawio")
    build_single_page_file(add_module_page, out_dir / "图4-2_系统功能模块图.drawio")

    print("drawio files generated:")
    for path in sorted(out_dir.glob("*.drawio")):
        print(path)


if __name__ == "__main__":
    main()
