package dev.dejohn.killer.cards;

import java.util.stream.IntStream;

import static dev.dejohn.killer.cards.Cards.NATURAL_COUNT;

public sealed interface Card {
    static Card get(int index) {
        return index < NATURAL_COUNT ? getNatural(index) : getWild(index);
    }

    static Card.Natural getNatural(int index) {
        return Natural.ALL[index];
    }

    static Card.Wild getWild(int index) {
        return Wild.values()[index - NATURAL_COUNT];
    }

    final class Natural implements Card, Comparable<Natural> {
        private static final Card.Natural[] ALL = IntStream.range(0, NATURAL_COUNT)
            .mapToObj(i -> new Natural(i, Rank.values()[i / 4], Suit.values()[i % 4]))
            .toArray(Card.Natural[]::new);

        public final int index;
        public final Rank rank;
        public final Suit suit;

        private Natural(int index, Rank rank, Suit suit) {
            this.index = index;
            this.rank = rank;
            this.suit = suit;
        }

        public Rank getRank() {
            return rank;
        }

        public Suit getSuit() {
            return suit;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public String getName() {
            return rank.name() + " of " + suit.name();
        }

        @Override
        public int compareTo(Natural other) {
            return rank.compareTo(other.rank);
        }

        @Override
        public String toString() {
            return "|" + rank.symbol + suit.symbol +  "|";
        }
    }

    enum Wild implements Card {
        FIRST, SECOND, THIRD, FOURTH, FIFTH, SIXTH, SEVENTH, EIGHTH, NINTH, TENTH, ELEVENTH, TWELFTH;

        public final int index;

        Wild() {
            index = NATURAL_COUNT + ordinal();
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public String getName() {
            return "JOKER (" + (ordinal() + 1) + ")";
        }

        @Override
        public String toString() {
            return "|jk|";
        }
    }

    String getName();
    int getIndex();

    default long getBitVector() {
        return 1L << getIndex();
    }
}
