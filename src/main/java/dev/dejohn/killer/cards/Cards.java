package dev.dejohn.killer.cards;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static dev.dejohn.killer.cards.Rank.TWO;
import static dev.dejohn.killer.cards.Suit.HEARTS;


public abstract sealed class Cards<T extends Card> implements Iterable<T> permits Cards.Any, Cards.Naturals, Cards.Wilds {
    public static final int NATURAL_COUNT = Rank.values().length * Suit.values().length;
    private static final long ALL_THREES_VECTOR = 15L;
    private static final long ALL_CLUBS_VECTOR = 300239975158033L;

    private static final int SINGLE_DEPTH = 1;
    private static final int PAIR_DEPTH = 2;
    private static final int TRIPLE_DEPTH = 3;
    private static final int QUAD_DEPTH = 4;

    public static Cards<Card> all() {
        return Any.ALL;
    }

    public static Cards<Card.Natural> natural() {
        return Naturals.ALL;
    }

    public static Cards<Card.Wild> wild() {
        return Wilds.ALL;
    }

    public static Cards<?> of(Card... cards) {
        return collect(Stream.of(cards));
    }

    public static Cards<Card.Natural> ofNatural(Card.Natural... cards) {
        return collectNatural(Stream.of(cards));
    }

    public static Cards<Card.Wild> ofWild(Card.Wild... cards) {
        return collectWild(Stream.of(cards));
    }

    public static Cards<?> collect(Stream<? extends Card> cards) {
        return from(cards.mapToLong(Card::getBitVector).reduce(0L, (a, b) -> a | b));
    }

    public static Cards<Card.Natural> collectNatural(Stream<Card.Natural> cards) {
        return Naturals.fromNatural(cards.mapToLong(Card::getBitVector).reduce(0L, (a, b) -> a | b));
    }

    public static Cards<Card.Wild> collectWild(Stream<Card.Wild> cards) {
        return Wilds.fromWild(cards.mapToLong(Card::getBitVector).reduce(0L, (a, b) -> a | b));
    }

    public static Cards<?> from(long bitVector) {
        if (bitVector == 0) return Any.EMPTY;
        if ((bitVector & Naturals.ALL.bitVector) == bitVector) return Naturals.fromNatural(bitVector);
        if ((bitVector & Wilds.ALL.bitVector) == bitVector) return Wilds.fromWild(bitVector);
        return new Any(bitVector);
    }

    public static final class Any extends Cards<Card> {
        private static final Any ALL = new Any(-1L);
        private static final Any EMPTY = new Any(0);

        private Any(long bitVector) {
            super(bitVector);
        }

        @Override
        Card getCard(int index) {
            return Card.get(index);
        }

        @Override
        Cards<Card> supply(long bitVector) {
            return (Cards<Card>) from(bitVector);
        }
    }

    public static final class Naturals extends Cards<Card.Natural> {
        private static final Naturals ALL = new Naturals(-1L >>> (Long.SIZE - NATURAL_COUNT));
        private static final Naturals EMPTY = new Naturals(0);

        private static Naturals fromNatural(long bitVector) {
            if (bitVector == 0) return Naturals.EMPTY;
            if (bitVector == Naturals.ALL.bitVector) return Naturals.ALL;
            return new Naturals(bitVector);
        }

        private Naturals(long bitVector) {
            super(bitVector);
        }

        @Override
        Card.Natural getCard(int index) {
            return Card.getNatural(index);
        }

        @Override
        Naturals supply(long bitVector) {
            return Naturals.fromNatural(bitVector);
        }

        @Override
        public Naturals naturals() {
            return this;
        }
    }

    public static final class Wilds extends Cards<Card.Wild> {
        private static final Wilds ALL = new Wilds(-1L << NATURAL_COUNT);
        private static final Wilds EMPTY = new Wilds(0);

        private static Wilds fromWild(long bitVector) {
            if (bitVector == 0) return Wilds.EMPTY;
            if (bitVector == Wilds.ALL.bitVector) return Wilds.ALL;
            return new Wilds(bitVector);
        }

        private Wilds(long bitVector) {
            super(bitVector);
        }

        @Override
        Card.Wild getCard(int index) {
            return Card.getWild(index);
        }

        @Override
        Wilds supply(long bitVector) {
            return Wilds.fromWild(bitVector);
        }

        @Override
        public Wilds wilds() {
            return this;
        }
    }

    public final long bitVector;

    private Cards(long bitVector) {
        this.bitVector = bitVector;
    }

    abstract T getCard(int index);
    abstract Cards<T> supply(long bitVector);

    public boolean contains(long bitVector) {
        return (this.bitVector & bitVector) == bitVector;
    }

    public boolean intersects(long bitVector) {
        return (this.bitVector & bitVector) != 0;
    }

    public Cards<?> add(long bitVector) {
        return contains(bitVector) ? this : Any.from(this.bitVector | bitVector);
    }

    public Cards<T> remove(long bitVector) {
        return !intersects(bitVector) ? this : supply(this.bitVector & ~bitVector);
    }

    public Cards<T> intersection(long bitVector) {
        return this.bitVector == bitVector ? this : supply(this.bitVector & bitVector);
    }

    public long getBitVector() {
        return bitVector;
    }

    public int size() {
        return Long.bitCount(bitVector);
    }

    public boolean isEmpty() {
        return bitVector == 0;
    }

    public boolean contains(Card card) {
        return contains(card.getBitVector());
    }

    public boolean contains(Cards<?> cards) {
        return contains(cards.bitVector);
    }

    public boolean intersects(Card card) {
        return intersects(card.getBitVector());
    }

    public boolean intersects(Cards<?> cards) {
        return intersects(cards.bitVector);
    }

    public Cards<?> add(Card card) {
        return add(card.getBitVector());
    }

    public Cards<?> add(Cards<?> cards) {
        return add(cards.bitVector);
    }

    public Cards<T> remove(Card card) {
        return remove(card.getBitVector());
    }

    public Cards<T> remove(Cards<?> cards) {
        return remove(cards.bitVector);
    }

    public Cards<T> intersection(Card card) {
        return intersection(card.getBitVector());
    }

    public Cards<T> intersection(Cards<?> cards) {
        return intersection(cards.bitVector);
    }

    public Cards<Card.Natural> naturals() {
        return Naturals.fromNatural(remove(wild()).bitVector);
    }

    public Cards<Card.Wild> wilds() {
        return Wilds.fromWild(remove(natural()).bitVector);
    }

    public Cards<Card.Natural> filter(Rank rank) {
        return Naturals.fromNatural(bitVector & (ALL_THREES_VECTOR << rank.offset));
    }

    public Cards<Card.Natural> filter(Suit suit) {
        return Naturals.fromNatural(bitVector & (ALL_CLUBS_VECTOR << suit.ordinal()));
    }

    public int count(Rank rank) {
        return Long.bitCount(bitVector & (ALL_THREES_VECTOR << rank.offset));
    }

    public int count(Suit suit) {
        return Long.bitCount(bitVector & (ALL_CLUBS_VECTOR << suit.ordinal()));
    }

    public LongStream bitStream() {
        return LongStream.iterate(bitVector, b -> b != 0, b -> b ^ (b & -b))
            .map(b -> b & -b);
    }

    public Stream<T> stream() {
        return LongStream.iterate(bitVector, b -> b != 0, b -> b ^ (b & -b))
            .mapToObj(b -> getCard(Long.numberOfTrailingZeros(b)));
    }

    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Cards<?> c && bitVector == c.bitVector;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bitVector);
    }

    @Override
    public String toString() {
        return stream().map(Card::toString).collect(Collectors.joining(" "));
    }
}
