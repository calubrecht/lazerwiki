package us.calubrecht.lazerwiki.syntax.nodes;

public class TableNode extends ContainerNode{

    public static class TableRowNode extends ContainerNode {
    }

    public static class TableCellNode extends ContainerNode {

        public enum CELL_TYPE {HEADER, DATA, ROWSPAN_MARKER}
        public enum ALIGNMENT {NONE, LEFT, RIGHT, CENTER}

        final CELL_TYPE type;
        ALIGNMENT alignment = ALIGNMENT.NONE;

        int colSpan = 1;
        int rowSpan = 1;

        public TableCellNode(CELL_TYPE type) {
            this.type = type;
        }

        public CELL_TYPE getType() {
            return type;
        }

        public ALIGNMENT getAlignment() {
            return alignment;
        }

        public void setAlignment(ALIGNMENT alignment) {
            this.alignment = alignment;
        }

        public int getColSpan() {
            return colSpan;
        }

        public void setColSpan(int colSpan) {
            this.colSpan = colSpan;
        }

        public int getRowSpan() {
            return rowSpan;
        }

        public void setRowSpan(int rowSpan) {
            this.rowSpan = rowSpan;
        }
    }
}
