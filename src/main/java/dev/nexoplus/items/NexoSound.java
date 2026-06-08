package dev.nexoplus.items;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import java.util.List;

@Data
@Builder
public class NexoSound {
    private final String id;
    private final String namespace;
    private final String soundName;
    private String category;
    @Singular
    private List<String> files;
    private String subtitle;
}
