package dev.dejohn.killer;

import dev.dejohn.killer.cards.Card.Natural;
import dev.dejohn.killer.cards.Cards;
import dev.dejohn.killer.cards.Rank;
import dev.dejohn.killer.game.Action;
import dev.dejohn.killer.game.Context;

import java.util.stream.Stream;

public interface Brain {
    <T> Action.Play<T> getAction(Context context, Stream<? extends Action.Play<T>> options);
    <T> Action<T> getAction(Context context, Action.Play<?> toBeat, Stream<? extends Action.Play<T>> options);
}
