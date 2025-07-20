package dev.dejohn.killer.game;

import dev.dejohn.killer.cards.Card;
import dev.dejohn.killer.cards.Cards;
import dev.dejohn.killer.cards.Deck;

import java.util.stream.Stream;

public record Context(Deck deck, Cards<?> pile, Cards<?> hand, Stream<Opponent> opponents) {
    public record Opponent(boolean isActive, int cardCount) {}
}
