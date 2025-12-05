package com.cryptobot.model;

import java.time.Instant;

public record PricePoint(Instant dateTime, double price) {}
