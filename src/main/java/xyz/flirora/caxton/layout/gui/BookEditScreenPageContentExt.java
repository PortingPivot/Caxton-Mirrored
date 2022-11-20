package xyz.flirora.caxton.layout.gui;

import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.ForwardTraversedMap;

import java.util.List;

public interface BookEditScreenPageContentExt {
    List<CaxtonText> getCaxtonText();

    void setCaxtonText(List<CaxtonText> texts);

    ForwardTraversedMap getWarts();

    void setWarts(ForwardTraversedMap warts);
}
