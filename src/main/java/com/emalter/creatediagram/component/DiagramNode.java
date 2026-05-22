package com.emalter.creatediagram.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * Represents a node in the diagram with positional, visual and semantic properties.
 * The record contains fields for id, item type, position, property metadata, amount, color and sizing.
 */
public record DiagramNode(UUID id, String itemType, int x, int y, String property, int amount, int color, int width, int height) {

    /**
     * Compatibility constructor for older saves that did not include amount and color.
     */
    public DiagramNode(UUID id, String itemType, int x, int y, String property) {
        this(id, itemType, x, y, property, 1, 0xFFFFFF, getDefaultWidth(itemType), getDefaultHeight(itemType));
    }

    /**
     * Compatibility constructor for older saves that did not include width and height.
     */
    public DiagramNode(UUID id, String itemType, int x, int y, String property, int amount, int color) {
        this(id, itemType, x, y, property, amount, color, getDefaultWidth(itemType), getDefaultHeight(itemType));
    }

    private static int getDefaultWidth(String type) {
        return type.equals("creatediagram:text_comment") ? 80 : 40;
    }

    private static int getDefaultHeight(String type) {
        if (type.equals("creatediagram:text_comment")) return 20;;
        return type.contains("mechanical_mixer") ? 60 : 40;
    }

    // Codec used for disk serialization (NBT/JSON)
    public static final Codec<DiagramNode> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("id").forGetter(DiagramNode::id),
                    Codec.STRING.fieldOf("item_type").forGetter(DiagramNode::itemType),
                    Codec.INT.fieldOf("x").forGetter(DiagramNode::x),
                    Codec.INT.fieldOf("y").forGetter(DiagramNode::y),
                    Codec.STRING.fieldOf("property").forGetter(DiagramNode::property),
                    Codec.INT.optionalFieldOf("amount", 1).forGetter(DiagramNode::amount),
                    Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(DiagramNode::color),
                    // If absent in the file use 0 (decoder will substitute defaults)
                    Codec.INT.optionalFieldOf("width", 0).forGetter(DiagramNode::width),
                    Codec.INT.optionalFieldOf("height", 0).forGetter(DiagramNode::height)
            ).apply(instance, (id, itemType, x, y, property, amount, color, w, h) -> {
                int finalW = (w == 0) ? getDefaultWidth(itemType) : w;
                int finalH = (h == 0) ? getDefaultHeight(itemType) : h;
                return new DiagramNode(id, itemType, x, y, property, amount, color, finalW, finalH);
            })
    );

    // Manual network serialization to avoid composite() parameter limit
    public static final StreamCodec<RegistryFriendlyByteBuf, DiagramNode> STREAM_CODEC = StreamCodec.of(
            (buf, node) -> {
                buf.writeUUID(node.id());
                buf.writeUtf(node.itemType());
                buf.writeInt(node.x());
                buf.writeInt(node.y());
                buf.writeUtf(node.property());
                buf.writeInt(node.amount());
                buf.writeInt(node.color());
                        buf.writeInt(node.width());
                        buf.writeInt(node.height());
            },
            buf -> {
                return new DiagramNode(
                        buf.readUUID(),
                        buf.readUtf(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readUtf(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt()
                );
            }
    );
}