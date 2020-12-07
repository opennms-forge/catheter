package org.opennms.nephron.catheter.random;

import java.util.Random;

public class IntegerZufall extends Zufall<Integer> {
    public IntegerZufall(final Random random, final Integer min, final Integer max) {
        super(random, min, max);
    }

    @Override
    protected long toLong(final Integer integer) {
        return integer;
    }

    @Override
    protected Integer fromLong(final long aLong) {
        return (int) aLong;
    }
}
