package gg.xp.xivsupport.gui.tables.renderers;

import java.awt.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// https://tips4java.wordpress.com/2009/07/26/overlap-layout/
public class OverlapLayout implements LayoutManager2, Serializable {
	public static final Boolean POP_UP = Boolean.TRUE;
	public static final Boolean POP_DOWN = Boolean.FALSE;

	private static final int PREFERRED = 0;
	private static final int MINIMUM = 1;
	@Serial
	private static final long serialVersionUID = 1200924519140167411L;

	//  Indicates how a component is painted
	private final boolean overlapAbove;

	private Point overlapPosition;

	//  Reserve space for invisible components in the Container
	private boolean includeInvisible = true;

	//  Reserve extra space so a component can "popup"
	private Insets popupInsets = new Insets(0, 0, 0, 0);

	//  Track original order in which the components where added
	private final List<Component> components = new ArrayList<>();

	//  Track a constraint added to a component
	private final HashMap<Component, Boolean> constraints = new HashMap<>();

	/**
	 * Convenience constructor to provide for "stacking" of components. Each
	 * component will be stacked above the previous component and sized to
	 * fill the space of the parent container.
	 */
	public OverlapLayout() {
		this(new Point(0, 0));
	}

	/**
	 * Convenience constructor. Each component will overlap above the previous
	 * component.
	 *
	 * @param overlapPosition a Point defining the relative amount of overlap
	 */
	public OverlapLayout(Point overlapPosition) {
		this(overlapPosition, true);
	}

	/**
	 * Create an overlapping layout.
	 *
	 * @param overlapPosition a Point defining the relative amount of overlap
	 * @param overlapAbove    when true components are painted above the previous
	 *                        component, otherwise they are painted below.
	 */
	public OverlapLayout(Point overlapPosition, boolean overlapAbove) {
		setOverlapPosition(overlapPosition);
		this.overlapAbove = overlapAbove;
	}

	/**
	 * When components are overlapped above the ZOrder of each component is
	 * changed resulting in the components position in the container being
	 * changed. For example when you add a component to the end of the
	 * container it will be moved to the beginning. If you then try to access
	 * the component using Component.componentAt(), you will get the first
	 * component, not the last.
	 * <p>
	 * This method will convert the index to you access the proper component.
	 *
	 * @param index the index to convert
	 */
	public int convertIndex(int index) {
		if (overlapAbove)
			return components.size() - index - 1;
		else
			return index;
	}

	/**
	 * Get the include invisible property
	 *
	 * @return the include invisible property
	 */
	public boolean isIncludeInvisible() {
		return includeInvisible;
	}

	/**
	 * Controls whether spaces should reserved for invisible components in
	 * the container
	 *
	 * @param includeInvisible when true, space is reserved otherwise the
	 *                         component is not included in the layout sizing
	 */
	public void setIncludeInvisible(boolean includeInvisible) {
		this.includeInvisible = includeInvisible;
	}

	/**
	 * Get the overlapping position of each component
	 *
	 * @return the Point representing the overlapped position
	 */
	public Point getOverlapPosition() {
		return overlapPosition;
	}

	/**
	 * Specify the position where the overlapped component should be painted.
	 *
	 * @param overlapPosition the position where the next component is painted
	 */
	public void setOverlapPosition(Point overlapPosition) {
		this.overlapPosition = overlapPosition;
	}

	/**
	 * Get the popup insets
	 *
	 * @return the popup insets
	 */
	public Insets getPopupInsets() {
		return popupInsets;
	}

	/**
	 * Define extra space to be reserved by the container. This will allow
	 * components to be "popped up" if required. Generally space would only be
	 * reserved on one side of the container.
	 *
	 * @param popupInsets Insets defining extra space for a particular side
	 *                    of the container.
	 */
	public void setPopupInsets(Insets popupInsets) {
		this.popupInsets = popupInsets;
	}

	/**
	 * Gets the constraints for the specified component.
	 *
	 * @param component the component to be queried
	 * @return the constraint for the specified component, or null
	 * if component is null or is not present in this layout
	 */
	public Boolean getConstraints(Component component) {
		return (Boolean) constraints.get(component);
	}

	/**
	 * Adds the specified component with the specified name to the layout.
	 *
	 * @param name the name of the component
	 * @param comp the component to be added
	 */
	@Override
	public void addLayoutComponent(String name, Component comp) {
	}

	/*
	 *	Keep track of any specified constraint for the component.
	 */
	@Override
	public void addLayoutComponent(Component component, Object constraint) {
		//  Support simple Boolean constraint for painting a Component in
		//  its "popped up" position

		if (constraint == null) {
			constraints.remove(component);
		}
		else if (constraint instanceof Boolean bool) {
			constraints.put(component, bool);
		}
		else {
			String message = "Constraint parameter must be of type Boolean";
			throw new IllegalArgumentException(message);
		}

		//  Keep a separate List of components in the order in which they where
		//  added to the Container. This makes layout easier. First we need
		//  to find the position the component was added at. We can't depend
		//  on the component order in the parent Container as changing the
		//  Z-Order also changes the order in the Container

		if (!components.contains(component)) {
			Container parent = component.getParent();
			int size = parent.getComponentCount();

			for (int i = 0; i < size; i++) {
				Component c = parent.getComponent(i);

				if (c == component) {
					components.add(i, component);

					//  Need to change Z-Order so added components are painted
					//  above the previously added component.

					if (overlapAbove) {
						parent.setComponentZOrder(component, size - i - 1);
					}

					break;
				}
			}
		}
	}

	/**
	 * Removes the specified component from the layout.
	 *
	 * @param component the component to be removed
	 */
	@Override
	public void removeLayoutComponent(Component component) {
		components.remove(component);
		constraints.remove(component);
	}

	/**
	 * Determine the minimum size on the Container
	 *
	 * @param parent the container in which to do the layout
	 * @return the minimum dimensions needed to lay out the
	 * subcomponents of the specified container
	 */
	@Override
	public Dimension minimumLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			return getLayoutSize(parent, MINIMUM);
		}
	}

	/**
	 * Determine the preferred size on the Container
	 *
	 * @param parent the container in which to do the layout
	 * @return the preferred dimensions to lay out the
	 * subcomponents of the specified container
	 */
	@Override
	public Dimension preferredLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			return getLayoutSize(parent, PREFERRED);
		}
	}

	/*
	 *  The calculation for minimum/preferred size it the same. The only
	 *  difference is the need to use the minimum or preferred size of the
	 *  component in the calculation.
	 *
	 *  @param	 parent  the container in which to do the layout
	 *  @param	 type    either MINIMUM or PREFERRED
	 */
	private Dimension getLayoutSize(Container parent, int type) {
		int visibleComponents = 0;
		int width = 0;
		int height = 0;

		//  All components will be resized to the maximum dimension

		for (Component component : components) {
			if (component.isVisible()
					|| includeInvisible) {
				Dimension size = getDimension(component, type);
				width = Math.max(size.width, width);
				height = Math.max(size.height, height);
				visibleComponents++;
			}
		}

		if (visibleComponents == 0)
			return new Dimension(0, 0);

		//  Adjust size for each overlapping component

		visibleComponents--;
		width += visibleComponents * Math.abs(overlapPosition.x);
		height += visibleComponents * Math.abs(overlapPosition.y);

		//  Adjust for parent Container and popup insets

		Insets parentInsets = parent.getInsets();
		width += parentInsets.left + parentInsets.right;
		height += parentInsets.top + parentInsets.bottom;

		width += popupInsets.left + popupInsets.right;
		height += popupInsets.top + popupInsets.bottom;

		return new Dimension(width, height);
	}

	private Dimension getDimension(Component component, int type) {
		return switch (type) {
			case PREFERRED -> component.getPreferredSize();
			case MINIMUM -> component.getMinimumSize();
			default -> new Dimension(0, 0);
		};
	}

	/**
	 * Lays out the specified container using this layout.
	 * <p>
	 *
	 * @param parent the container in which to do the layout
	 */
	@Override
	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			int size = components.size();

			if (size == 0) return;

			//  Determine the maximum size of the components

			Dimension maximumSize = new Dimension();

			for (Component component : components) {
				if (component.isVisible()
						|| includeInvisible) {
					Dimension preferred = component.getPreferredSize();
					maximumSize.width = Math.max(preferred.width, maximumSize.width);
					maximumSize.height = Math.max(preferred.height, maximumSize.height);
				}
			}

			//  Determine location of first component

			Point location = new Point(0, 0);
			Insets parentInsets = parent.getInsets();

			//  Layout right-to-left, else left-to-right

			if (overlapPosition.x < 0)
				location.x = parent.getWidth() - maximumSize.width - parentInsets.right - popupInsets.right;
			else
				location.x = parentInsets.left + popupInsets.left;

			//  Layout bottom-to-top, else top-to-bottom

			if (overlapPosition.y < 0)
				location.y = parent.getHeight() - maximumSize.height - parentInsets.bottom - popupInsets.bottom;
			else
				location.y = parentInsets.top + popupInsets.top;

			//  Set the size and location for each component

			for (int i = 0; i < size; i++) {
				Component component = components.get(i);

				if (component.isVisible()
						|| includeInvisible) {
					//  When components are "stacked" resize each component to fill
					//  the size of the parent container

					if (overlapPosition.x == 0 && overlapPosition.y == 0) {
						int width = parent.getWidth() - parentInsets.left - parentInsets.right;
						int height = parent.getHeight() - parentInsets.top - parentInsets.bottom;
						component.setSize(width, height);
					}
					else  //  resize each component to be the same size
					{
						component.setSize(maximumSize);
					}

					//  Set location of the component

					int x = location.x;
					int y = location.y;

					//  Adjust location when component is "popped up"

					Boolean constraint = constraints.get(component);

					if (constraint == Boolean.TRUE) {
						x += popupInsets.right - popupInsets.left;
						y += popupInsets.bottom - popupInsets.top;
					}

					component.setLocation(x, y);

					//  Calculate location of next component using the overlap offsets

					location.x += overlapPosition.x;
					location.y += overlapPosition.y;
				}
			}
		}
	}

	/**
	 * There is no maximum.
	 */
	@Override
	public Dimension maximumLayoutSize(Container target) {
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	/**
	 * Returns the alignment along the x axis.  Use center alignment.
	 */
	@Override
	public float getLayoutAlignmentX(Container parent) {
		return 0.5f;
	}

	/**
	 * Returns the alignment along the y axis.  Use center alignment.
	 */
	@Override
	public float getLayoutAlignmentY(Container parent) {
		return 0.5f;
	}

	/**
	 * Invalidates the layout, indicating that if the layout manager
	 * has cached information it should be discarded.
	 */
	@Override
	public void invalidateLayout(Container target) {
		// remove constraints here?
	}

	/**
	 * Returns the string representation of this column layout's values.
	 *
	 * @return a string representation of this grid layout
	 */
	public String toString() {
		return getClass().getName()
				+ "[overlapAbove=" + overlapAbove
				+ ",overlapPosition=" + overlapPosition
				+ "]";
	}
}
