package com.emalter.creatediagram.logic;

import java.util.List;

public record BatchResult(int batches, RecipeType type, List<String> infiniteInputs) {}
