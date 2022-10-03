/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rocks.tbog.touchblue.helpers;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private static final HashMap<UUID, String> attributes = new HashMap<>();
    public static final String CCCD = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static final String SERVICE_LED = "54B10000-5442-6f67-9000-cc505effcd37";
    public static final String LED_SWITCH = "54B10001-5442-6f67-9000-cc505effcd37";
    public static final String LED_BRIGHTNESS = "54B10002-5442-6f67-9000-cc505effcd37";
    public static final String LED_SATURATION = "54B10003-5442-6f67-9000-cc505effcd37";
    public static final String SERVICE_ACCEL = "54B20000-5442-6f67-9000-cc505effcd37";
    public static final String ACCEL_RANGE = "54B10004-5442-6f67-9000-cc505effcd37";
    public static final String ACCEL_BANDWIDTH = "54B10005-5442-6f67-9000-cc505effcd37";
    public static final String ACCEL_SAMPLE_RATE = "54B10006-5442-6f67-9000-cc505effcd37";
    public static final String TAP_COUNT = "54B10007-5442-6f67-9000-cc505effcd37";
    public static final String GAME_STATE = "54B10008-5442-6F67-9000-CC505EFFCD37";

    static {
        // Sample Services.
        addAttribute("00001800-0000-1000-8000-00805f9b34fb", "Generic Access Service"); // will probably contain `appearance` and `device name` characteristics
        addAttribute("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute Service"); // will probably contain `service changed` characteristic
        addAttribute("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        addAttribute("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        addAttribute("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service");

        // Sample Characteristics.
        addAttribute("00002a00-0000-1000-8000-00805f9b34fb", "Device Name");
        addAttribute("00002a01-0000-1000-8000-00805f9b34fb", "Appearance");
        addAttribute("00002a05-0000-1000-8000-00805f9b34fb", "Service Changed");
        addAttribute(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        addAttribute("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level");
        addAttribute("00002a24-0000-1000-8000-00805f9b34fb", "Model Number String");
        addAttribute("00002a25-0000-1000-8000-00805f9b34fb", "Serial Number String");
        addAttribute("00002a26-0000-1000-8000-00805f9b34fb", "Firmware Revision String");
        addAttribute("00002a27-0000-1000-8000-00805f9b34fb", "Hardware Revision String");
        addAttribute("00002a28-0000-1000-8000-00805f9b34fb", "Software Revision String");
        addAttribute("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");

        // LED Service
        addAttribute(SERVICE_LED, "LED Service");
        addAttribute(LED_SWITCH, "LED on/off");
        addAttribute(LED_BRIGHTNESS, "LED brightness");
        addAttribute(LED_SATURATION, "LED saturation");

        // Accelerometer Service
        addAttribute(SERVICE_ACCEL, "Accelerometer Service");
        addAttribute(ACCEL_RANGE, "Accel range"); // 2g, 4g, 8g, 16g
        addAttribute(ACCEL_BANDWIDTH, "Accel bandwidth"); // (Hz) 50, 100, 200, 400
        addAttribute(ACCEL_SAMPLE_RATE, "Accel sample rate"); // (Hz) 13,26,52,104,208,416,833,1660,3330,6660,13330
        addAttribute(TAP_COUNT, "tap count");
        addAttribute(GAME_STATE, "game state");
    }

    public static void addAttribute(@NonNull final String uuidString, @NonNull final String name) {
        attributes.put(UUID.fromString(uuidString), name);
    }

    public static String lookup(UUID uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static String lookup(String uuidString, String defaultName) {
        return lookup(UUID.fromString(uuidString), defaultName);
    }
}
