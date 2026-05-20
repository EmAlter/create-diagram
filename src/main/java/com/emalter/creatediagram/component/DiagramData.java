package com.emalter.creatediagram.component;

import com.emalter.creatediagram.view.widget.CanvasPanel;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.UUID;

public record DiagramData(
        List<DiagramNode> nodes,
        List<DiagramEdge> edges,
        List<CanvasPanel.DiagramStroke> strokes
) {
    // 1. Codec di utilità per il singolo punto [x, y] rappresentato come array di int
    private static final Codec<int[]> POINT_CODEC = Codec.INT.listOf().xmap(
            list -> new int[]{list.get(0), list.get(1)},
            array -> List.of(array[0], array[1])
    );

    // 2. Codec per ricostruire e salvare la struttura del DiagramStroke
    private static final Codec<CanvasPanel.DiagramStroke> STROKE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(stroke -> stroke.id() != null ? stroke.id() : UUID.randomUUID()),
            Codec.INT.fieldOf("color").forGetter(CanvasPanel.DiagramStroke::color),
            POINT_CODEC.listOf().fieldOf("points").forGetter(CanvasPanel.DiagramStroke::points)
    ).apply(instance, CanvasPanel.DiagramStroke::new));

    // 3. Il Codec principale letto dal Data Component e agganciato al sistema di Networking
    public static final Codec<DiagramData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DiagramNode.CODEC.listOf().fieldOf("nodes").forGetter(DiagramData::nodes),
            DiagramEdge.CODEC.listOf().fieldOf("edges").forGetter(DiagramData::edges),
            STROKE_CODEC.listOf().fieldOf("strokes").forGetter(DiagramData::strokes)
    ).apply(instance, DiagramData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiagramData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
}