package cn.vahoa.draw.infrastructure.security;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DrawBehavior {

    private long timestamp;

    private int hour;

    private boolean referer;

    private boolean pageView;

    public boolean hasReferer() {
        return referer;
    }

    public boolean hasPageView() {
        return pageView;
    }
}
