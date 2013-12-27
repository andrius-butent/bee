package com.butent.bee.shared.websocket.messages;

import com.google.common.collect.Lists;

import com.butent.bee.shared.communication.ChatRoom;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.List;

public class RoomsMessage extends Message {

  private final List<ChatRoom> data = Lists.newArrayList();

  public RoomsMessage(Collection<ChatRoom> data) {
    this();
    this.data.addAll(data);
  }

  RoomsMessage() {
    super(Type.ROOMS);
  }

  public List<ChatRoom> getData() {
    return data;
  }

  @Override
  public String toString() {
    return BeeUtils.joinOptions("type", string(getType()), "size", BeeUtils.toString(data.size()),
        "rooms", data.isEmpty() ? null : data.toString());
  }

  @Override
  protected void deserialize(String s) {
    if (!data.isEmpty()) {
      data.clear();
    }

    String[] arr = Codec.beeDeserializeCollection(s);
    if (arr != null) {
      for (String rd : arr) {
        data.add(ChatRoom.restore(rd));
      }
    }
  }

  @Override
  protected String serialize() {
    return Codec.beeSerialize(getData());
  }
}
