package com.cryptobot.model;

import java.time.ZonedDateTime;

public record PricePoint(ZonedDateTime dateTime, double price) {}