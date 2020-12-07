/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.nephron.catheter.random;

import java.util.Objects;
import java.util.Random;

public abstract class Zufall<T> {
    private final long start;
    private final long range;
    private final Random random;

    public Zufall(final Random random, final T min, final T max) {
        this.start = this.toLong(min);
        this.range = this.toLong(max) - this.start;
        this.random = random;
    }

    public T random() {
        if (this.range == 0) {
            return fromLong(this.start);
        }
        final long l = this.start + Math.abs(random.nextLong()) % (this.range);
        return this.fromLong(l);
    }

    protected abstract long toLong(final T t);

    protected abstract T fromLong(final long l);

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Zufall<?> zufall = (Zufall<?>) o;
        return start == zufall.start &&
                range == zufall.range;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, range);
    }

    @Override
    public String toString() {
        return "Zufall{" +
                "start=" + start +
                ", range=" + range +
                '}';
    }
}
