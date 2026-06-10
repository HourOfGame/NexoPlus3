package dev.nexoplus.items;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import java.util.List;

@Data
@Builder
public class NexoFont {
    private final String id;
    private final String namespace;
    private final String fontName;
    private String type;      // "bitmap"
    private String file;      // path relative to textures/
    private int ascent;
    private int height;
    @Singular
    private List<String> chars;
}
