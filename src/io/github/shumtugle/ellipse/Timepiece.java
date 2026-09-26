package io.github.shumtugle.ellipse;

/** A clock the home screen can carry across its top: told whether to show the weather, and the headphones' charge. */
interface Timepiece {
    void weather(boolean shown);

    void ears(int level);

    /** Whether it reaches the screen's own edge, left and right: a face with a card runs its card to it. */
    default void edged(boolean left, boolean right) {
    }
}
