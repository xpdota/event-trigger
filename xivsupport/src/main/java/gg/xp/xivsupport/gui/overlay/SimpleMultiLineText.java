package gg.xp.xivsupport.gui.overlay;

import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SimpleMultiLineText extends Component {

	private volatile String input;
	private volatile @Nullable Output output;
	private int extraYpad;
	private TextAlignment alignment = TextAlignment.LEFT;

	public void setText(String text) {
		if (!Objects.equals(input, text)) {
			this.input = text;
			recalc();
		}
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		recalc();
	}

	public void setAlignment(TextAlignment alignment) {
		this.alignment = alignment;
		recalc();
	}

	public void setExtraYpad(int extraYpad) {
		this.extraYpad = extraYpad;
		recalc();
	}

	@Override
	public void setFont(Font f) {
		super.setFont(f);
		recalc();
	}

	@Override
	public void setForeground(Color c) {
		super.setForeground(c);
		recalc();
	}

	private record Output(int width, int height, List<TextLayout> lines) {
	}

	private void recalc() {
		String text = input;
		if (text == null || text.isBlank()) {
			output = null;
			return;
		}
		FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
		AttributedString str = new AttributedString(text);
		Font font = getFont();
		if (font != null) {
			str.addAttribute(TextAttribute.FONT, font);
		}
		LineBreakMeasurer lbm = new LineBreakMeasurer(str.getIterator(), frc);
		int width = getWidth();
		if (width <= 0) {
			return;
		}
		int height = 0;
		int maxLineWidth = 0;
		List<TextLayout> lines = new ArrayList<>();
		while (lbm.getPosition() < text.length()) {
			TextLayout layout = lbm.nextLayout(width);
			height += layout.getAscent() + layout.getDescent();
			maxLineWidth = (int) Math.max(maxLineWidth, layout.getBounds().getWidth());
			lines.add(layout);
		}
		output = new Output(maxLineWidth, height, lines);
	}

	@Override
	public void paint(Graphics gg) {
		paintInternal(gg, getWidth());
	}

	public void paintMinimumSquare(Graphics gg) {
		paintInternal(gg, getTextWidth());
	}

	private void paintInternal(Graphics gg, int paintingWidth) {
		Output output = this.output;
		if (output == null) {
			return;
		}
		Graphics2D g = ((Graphics2D) gg);
		g.setFont(getFont());
		g.setColor(getForeground());
		List<TextLayout> lines = output.lines;
		int yPos = 0;
		for (TextLayout line : lines) {
			yPos += line.getAscent();
			Rectangle2D lineBounds = line.getBounds();
			double xPos = switch (alignment) {
				case LEFT -> 0;
				case CENTER -> paintingWidth / 2.0 - lineBounds.getWidth() / 2.0;
				case RIGHT -> paintingWidth - lineBounds.getWidth();
			};
			line.draw(g, (float) xPos, yPos);
			yPos += line.getDescent();
			yPos += extraYpad;
		}

	}

	public int getTextHeight() {
		Output output = this.output;
		return output == null ? 1 : output.height;
	}

	public int getTextWidth() {
		Output output = this.output;
		return output == null ? 1 : output.width;
	}

}
