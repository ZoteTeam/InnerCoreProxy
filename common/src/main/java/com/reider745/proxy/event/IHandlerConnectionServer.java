package com.reider745.proxy.event;

import com.reider745.proxy.network.Connection;

public interface IHandlerConnectionServer {
    void onConnect(Connection connection);
}
