package com.butent.bee.shared.websocket.messages;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.Chat;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class ChatStateMessage extends Message {

  public static ChatStateMessage add(Chat chat) {
    return (chat == null) ? null : new ChatStateMessage(chat, State.NEW);
  }

  public static ChatStateMessage remove(Chat chat) {
    return (chat == null) ? null : new ChatStateMessage(chat, State.REMOVED);
  }

  public static ChatStateMessage update(Chat chat) {
    return (chat == null) ? null : new ChatStateMessage(chat, State.UPDATING);
  }

  private Chat chat;
  private State state;

  private ChatStateMessage(Chat chat, State state) {
    this();

    this.chat = chat;
    this.state = state;
  }

  ChatStateMessage() {
    super(Type.CHAT_STATE);
  }

  @Override
  public String brief() {
    return string(getState());
  }

  public Chat getChat() {
    return chat;
  }

  public State getState() {
    return state;
  }

  public boolean isNew() {
    return getState() == State.NEW;
  }

  public boolean isRemoved() {
    return getState() == State.REMOVED;
  }

  public boolean isUpdated() {
    return getState() == State.UPDATING;
  }

  @Override
  public boolean isValid() {
    return getChat() != null && getState() != null;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("state", string(getState()),
        "chat", (getChat() == null) ? null : chat.toString());
  }

  @Override
  protected void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    setChat(Chat.restore(arr[0]));
    setState(Codec.unpack(State.class, arr[1]));
  }

  @Override
  protected String serialize() {
    List<Object> values = new ArrayList<>();

    values.add(getChat());
    values.add(Codec.pack(getState()));

    return Codec.beeSerialize(values);
  }

  private void setChat(Chat chat) {
    this.chat = chat;
  }

  private void setState(State state) {
    this.state = state;
  }
}
