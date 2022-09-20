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

package rocks.tbog.touchblue.ui;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static final HashMap<UUID, String> attributes = new HashMap<>();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String LED_SWITCH = "19b10001-e8f2-537e-4f6c-d104768a1214";

    static {
        // Sample Services.
        addAttribute("00001800-0000-1000-8000-00805f9b34fb", "Generic Access Service");
        addAttribute("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute Service");
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
        addAttribute("19b10000-e8f2-537e-4f6c-d104768a1214", "LED Service");
        addAttribute(LED_SWITCH, "LED Switch");
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
