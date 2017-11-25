package com.theKidOfArcrania.mips.highlight;

import com.theKidOfArcrania.mips.parsing.Range;

/**
 * Represents a tag highlight. This consists of a bubble/tooltip, and also a syntax highlight. This, like the
 * {@link Syntax} object, can provide hints to the UI as to how to colorize the text to make the code more appealing.
 * Specifically with this particular class, it can also provide the user with helpful feedback as
 * errors/warnings/info on the code.
 *
 * @author Henry Wang
 */
public class Tag extends HighlightMark<TagType> {
    private String tagDescription;

    /**
     * Creates a tag without a description
     *
     * @param type the type of tag
     * @param span the range span that this tag spans across.
     */
    public Tag(TagType type, Range span) {
        this(type, span, "");
    }

    /**
     * Creates a tag with a description.
     *
     * @param type           the type of the tag
     * @param span           the range span that this tag spans across.
     * @param tagDescription the tag description often shown in a tooltip/bubble.
     */
    public Tag(TagType type, Range span, String tagDescription) {
        super(type, span);
        if (tagDescription == null) {
            throw new NullPointerException();
        }

        this.tagDescription = tagDescription;
    }

    public String getTagDescription() {
        return tagDescription;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + tagDescription + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Tag tag = (Tag) o;

        return tagDescription.equals(tag.tagDescription);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + tagDescription.hashCode();
        return result;
    }
}
