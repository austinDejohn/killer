package dev.dejohn.killer.cards;

import dev.dejohn.killer.cards.Card.*;

import java.util.*;
import java.util.stream.Collectors;

public final class Deck {
    public static Deck natural() {
        return Deck.of(Cards.natural());
    }

    public static Deck upTo(Wild max) {
        return Deck.of(
            Cards.collect(Cards.wild().stream().filter(w -> w.compareTo(max) < 1))
                .add(Cards.natural())
        );
    }

    public static Deck of(Cards<?> cards) {
        return new Deck(cards, new Random());
    }

    public final Cards<?> cards;
    private final Random random;

    private Deck(Cards<?> cards, Random random) {
        this.cards = cards;
        this.random = random;
    }

    public int size() {
        return cards.size();
    }

    public Cards<Natural> naturals() {
        return cards.naturals();
    }

    public Cards<Wild> wilds() {
        return cards.wilds();
    }

    public List<Cards<?>> deal(int numberOfHands) {
        long[] shuffled = new long[cards.size()];
        long[] hands = new long[numberOfHands];
        long r = cards.bitVector, b;
        int h = 0;

        for (int i = 0; i < shuffled.length; i++) {
            int n = random.nextInt(i + 1);
            r ^= (b = r & -r);
            shuffled[i] = shuffled[n];
            shuffled[n] = b;
        }

        for (long bitVector : shuffled) {
            if (h == numberOfHands) h = 0;
            hands[h++] |= bitVector;
        }

        return Arrays.stream(hands).mapToObj(Cards::from).collect(Collectors.toCollection(ArrayList::new));
    }
}
