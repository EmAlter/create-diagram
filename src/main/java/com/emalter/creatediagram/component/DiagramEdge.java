package com.emalter.creatediagram.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record DiagramEdge(UUID fromNode, String outputItem, UUID toNode, int amount) {

    public static final Codec<DiagramEdge> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("source_id").forGetter(DiagramEdge::fromNode),
                    Codec.STRING.fieldOf("output_item").forGetter(DiagramEdge::outputItem),
                    UUIDUtil.CODEC.fieldOf("target_id").forGetter(DiagramEdge::toNode),
                    Codec.INT.fieldOf("amount").orElse(1).forGetter(DiagramEdge::amount)
            ).apply(instance, DiagramEdge::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DiagramEdge> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, DiagramEdge::fromNode,
            ByteBufCodecs.STRING_UTF8, DiagramEdge::outputItem,
            UUIDUtil.STREAM_CODEC, DiagramEdge::toNode,
            ByteBufCodecs.INT, DiagramEdge::amount,
            (fromNode, outputItem, toNode, amount) -> new DiagramEdge(fromNode, outputItem, toNode, amount)
    );
}