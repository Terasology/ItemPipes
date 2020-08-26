// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.components;

import org.terasology.entitySystem.Component;
import org.terasology.math.Side;

import java.util.List;

public class PipeConnectionComponent implements Component {
    List<Side> sides;
}
