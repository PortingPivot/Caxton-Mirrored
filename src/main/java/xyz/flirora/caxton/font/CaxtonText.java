package xyz.flirora.caxton.font;

import java.util.List;

/**
 * Represents text laid out by Caxton.
 *
 * @param runGroups The list of run groups in this text.
 */
public record CaxtonText(List<RunGroup> runGroups) {
}
