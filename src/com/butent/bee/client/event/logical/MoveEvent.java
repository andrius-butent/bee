package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.client.event.Modifiers;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.Assert;

public final class MoveEvent extends GwtEvent<MoveEvent.Handler> {

  @FunctionalInterface
  public interface Handler extends EventHandler {
    void onMove(MoveEvent event);
  }

  public interface HasMoveHandlers extends HasHandlers {
    HandlerRegistration addMoveHandler(Handler handler);
  }

  private enum State {
    MOVING, FINISHED
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fireFinish(HasMoveHandlers source, int deltaX, int deltaY,
                                                                              Modifiers modifiers) {
    Assert.notNull(source);
    source.fireEvent(new MoveEvent(State.FINISHED, deltaX, deltaY, modifiers));
  }

  public static void fireMove(HasMoveHandlers source, int deltaX, int deltaY, Modifiers modifiers) {
    Assert.notNull(source);
    source.fireEvent(new MoveEvent(State.MOVING, deltaX, deltaY, modifiers));
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final State state;

  private final int deltaX;
  private final int deltaY;
  private final Modifiers modifiers;

  private MoveEvent(State state, int deltaX, int deltaY, Modifiers modifiers) {
    super();
    this.state = state;

    this.deltaX = deltaX;
    this.deltaY = deltaY;
    this.modifiers = modifiers;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public int getDeltaX() {
    return deltaX;
  }

  public int getDeltaY() {
    return deltaY;
  }

  public Modifiers getModifiers() {
    return modifiers;
  }

  public Mover getMover() {
    if (getSource() instanceof Mover) {
      return (Mover) getSource();
    } else {
      return null;
    }
  }

  public boolean isFinished() {
    return State.FINISHED.equals(state);
  }

  public boolean isMoving() {
    return State.MOVING.equals(state);
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onMove(this);
  }
}
