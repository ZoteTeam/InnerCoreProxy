package com.reider745.proxy.waterzote.json;

import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class SeverDetectionJson {
    private boolean server;
    private int socket_port;
    private Map<String, Integer> biomes;
    private boolean socket_enabled;
}
