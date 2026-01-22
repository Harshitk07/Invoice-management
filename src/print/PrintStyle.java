package print;

import javafx.geometry.Insets;
import javafx.scene.text.Font;

public final class PrintStyle {

    private PrintStyle() {}

    /* ========= BORDERS ========= */

    public static final String BORDER_ALL =
            "-fx-border-color: black; -fx-border-width: 1;";

    public static final String BORDER_TOP =
            "-fx-border-color: black; -fx-border-width: 1 0 0 0;";

    public static final String BORDER_BOTTOM =
            "-fx-border-color: black; -fx-border-width: 0 0 1 0;";

    public static final String BORDER_VERTICAL =
            "-fx-border-color: black; -fx-border-width: 0 1 0 1;";

    public static final String BORDER_NO_SIDE =
            "-fx-border-color: black; -fx-border-width: 1 0 1 0;";

    public static final String NO_BORDER =
            "-fx-border-width: 0 0 0 0;";

    /* ========= PADDING ========= */

    public static final Insets CELL_PADDING =
            new Insets(2, 4, 2, 4);

    public static final Insets TOTAL_PADDING =
            new Insets(2, 6, 2, 6);

    public static final Insets BLOCK_PADDING =
            new Insets(5);

    /* ========= FONTS ========= */

    public static final Font TABLE_FONT =
            Font.font(11);

    public static final Font SMALL_FONT =
            Font.font(10);

    /* ========= SIZES ========= */

    public static final double LINE_THICKNESS = 1;
}

