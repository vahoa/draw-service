package cn.vahoa.draw.infrastructure.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AntiCheatResult {

    private final boolean allowed;

    private final boolean needCaptcha;

    private final String message;

    public static AntiCheatResult pass() {
        return new AntiCheatResult(true, false, null);
    }

    public static AntiCheatResult captcha(String message) {
        return new AntiCheatResult(false, true, message);
    }

    public static AntiCheatResult block(String message) {
        return new AntiCheatResult(false, false, message);
    }
}
