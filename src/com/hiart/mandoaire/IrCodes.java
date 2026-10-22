package com.hiart.mandoaire;

import java.util.ArrayList;

/**
 * Generadores de tramas IR para aires acondicionados de base Midea
 * (la mayoría de equipos Mundo Clima). Portado de IRremoteESP8266.
 *
 * Modos: 0 = Frío, 1 = Seco, 2 = Auto, 3 = Calor, 4 = Ventilador.
 */
final class IrCodes {
    static final int CARRIER_HZ = 38000;

    static final int MODE_COOL = 0, MODE_DRY = 1, MODE_AUTO = 2, MODE_HEAT = 3, MODE_FAN = 4;
    static final int TEMP_MIN = 17, TEMP_MAX = 30;

    private IrCodes() {}

    // ---------------- Coolix (24 bits, mandos Midea RG57 / R51) ----------------

    private static final int COOLIX_TICK = 276;
    private static final int COOLIX_BIT_MARK = 2 * COOLIX_TICK;
    private static final int COOLIX_ONE_SPACE = 6 * COOLIX_TICK;
    private static final int COOLIX_ZERO_SPACE = 2 * COOLIX_TICK;
    private static final int COOLIX_HDR_MARK = 17 * COOLIX_TICK;
    private static final int COOLIX_HDR_SPACE = 16 * COOLIX_TICK;
    private static final int COOLIX_MIN_GAP = 19 * COOLIX_TICK;

    static final int COOLIX_OFF = 0xB27BE0;

    private static final int[] COOLIX_TEMP_MAP = {
        0b0000, 0b0001, 0b0011, 0b0010, 0b0110, 0b0111, 0b0101,
        0b0100, 0b1100, 0b1101, 0b1001, 0b1000, 0b1010, 0b1011
    };
    private static final int COOLIX_FAN_TEMP_CODE = 0b1110;
    private static final int COOLIX_FAN_AUTO = 0b101;
    private static final int COOLIX_FAN_AUTO0 = 0b000;
    private static final int COOLIX_SENSOR_IGNORE = 0b11111;

    static int coolixState(int mode, int temp) {
        int fan = (mode == MODE_AUTO || mode == MODE_DRY) ? COOLIX_FAN_AUTO0 : COOLIX_FAN_AUTO;
        int tempCode = COOLIX_TEMP_MAP[clamp(temp) - TEMP_MIN];
        int nativeMode = mode;
        if (mode == MODE_FAN) {  // Ventilador es un caso especial de Seco.
            nativeMode = MODE_DRY;
            tempCode = COOLIX_FAN_TEMP_CODE;
        }
        return (0xB2 << 16) | (((fan << 5) | COOLIX_SENSOR_IGNORE) << 8)
                | (tempCode << 4) | (nativeMode << 2);
    }

    static int[] coolixPattern(int state) {
        ArrayList<Integer> p = new ArrayList<>();
        for (int r = 0; r < 2; r++) {  // El mando original lo envía dos veces.
            if (r > 0) p.add(COOLIX_MIN_GAP);
            p.add(COOLIX_HDR_MARK);
            p.add(COOLIX_HDR_SPACE);
            for (int shift = 16; shift >= 0; shift -= 8) {
                int b = (state >> shift) & 0xFF;
                addByte(p, b, COOLIX_BIT_MARK, COOLIX_ONE_SPACE, COOLIX_ZERO_SPACE);
                addByte(p, b ^ 0xFF, COOLIX_BIT_MARK, COOLIX_ONE_SPACE, COOLIX_ZERO_SPACE);
            }
            p.add(COOLIX_BIT_MARK);
        }
        return toArray(p);
    }

    // ---------------- Midea (48 bits, mandos RG66 / R09C y similares) ----------------

    private static final int MIDEA_TICK = 80;
    private static final int MIDEA_BIT_MARK = 7 * MIDEA_TICK;
    private static final int MIDEA_ONE_SPACE = 21 * MIDEA_TICK;
    private static final int MIDEA_ZERO_SPACE = 7 * MIDEA_TICK;
    private static final int MIDEA_HDR_MARK = 56 * MIDEA_TICK;
    private static final int MIDEA_HDR_SPACE = 56 * MIDEA_TICK;
    private static final int MIDEA_MIN_GAP = 70 * MIDEA_TICK;

    static long mideaState(boolean power, int mode, int temp) {
        long state = 0xA1L << 40;                       // Cabecera + tipo "comando"
        state |= (long) ((power ? 0x80 : 0) | mode) << 32;  // Ventilador en auto (0)
        state |= (long) (0x40 | (clamp(temp) - TEMP_MIN)) << 24;  // Celsius
        state |= 0xFFFFL << 8;                          // Sin sensor ni temporizador
        return state | mideaChecksum(state);
    }

    private static int mideaChecksum(long state) {
        int sum = 0;
        for (int i = 1; i <= 5; i++) sum += reverse8((int) (state >> (8 * i)) & 0xFF);
        return reverse8((256 - sum) & 0xFF);
    }

    static int[] mideaPattern(long state) {
        ArrayList<Integer> p = new ArrayList<>();
        long data = state;
        for (int half = 0; half < 2; half++) {  // Trama normal y luego invertida.
            if (half > 0) p.add(MIDEA_MIN_GAP);
            p.add(MIDEA_HDR_MARK);
            p.add(MIDEA_HDR_SPACE);
            for (int shift = 40; shift >= 0; shift -= 8) {
                addByte(p, (int) (data >> shift) & 0xFF,
                        MIDEA_BIT_MARK, MIDEA_ONE_SPACE, MIDEA_ZERO_SPACE);
            }
            p.add(MIDEA_BIT_MARK);
            data = ~data;
        }
        return toArray(p);
    }

    // ---------------- Utilidades ----------------

    private static void addByte(ArrayList<Integer> p, int b, int mark, int one, int zero) {
        for (int bit = 7; bit >= 0; bit--) {
            p.add(mark);
            p.add(((b >> bit) & 1) != 0 ? one : zero);
        }
    }

    private static int reverse8(int b) {
        return Integer.reverse(b) >>> 24;
    }

    private static int clamp(int temp) {
        return Math.max(TEMP_MIN, Math.min(TEMP_MAX, temp));
    }

    private static int[] toArray(ArrayList<Integer> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i);
        return out;
    }
}
