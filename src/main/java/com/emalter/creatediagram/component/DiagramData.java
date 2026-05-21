package com.emalter.creatediagram.component;

import com.emalter.creatediagram.client.diagram.canvas.CanvasModel;
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
        List<CanvasModel.DiagramStroke> strokes
) {
    // Utility codec for a single point [x, y] represented as an int array
    private static final Codec<int[]> POINT_CODEC = Codec.INT.listOf().xmap(
            list -> new int[]{list.get(0), list.get(1)},
            array -> List.of(array[0], array[1])
    );

    // Codec to (de)serialize a DiagramStroke structure
    private static final Codec<CanvasModel.DiagramStroke> STROKE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(stroke -> stroke.id() != null ? stroke.id() : UUID.randomUUID()),
            Codec.INT.fieldOf("color").forGetter(CanvasModel.DiagramStroke::color),
            POINT_CODEC.listOf().fieldOf("points").forGetter(CanvasModel.DiagramStroke::points)
    ).apply(instance, CanvasModel.DiagramStroke::new));

    // Main codec used by the data component and the networking system
    public static final Codec<DiagramData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DiagramNode.CODEC.listOf().fieldOf("nodes").forGetter(DiagramData::nodes),
            DiagramEdge.CODEC.listOf().fieldOf("edges").forGetter(DiagramData::edges),
            STROKE_CODEC.listOf().fieldOf("strokes").forGetter(DiagramData::strokes)
    ).apply(instance, DiagramData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiagramData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
}