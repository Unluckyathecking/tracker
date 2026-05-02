/*
 * The tracker package defines a set of video/image analysis tools
 * built on the Open Source Physics framework by Wolfgang Christian.
 *
 * Copyright (c) 2024 Douglas Brown, Wolfgang Christian, Robert M. Hanson
 *
 * Tracker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tracker; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at <http://www.gnu.org/copyleft/gpl.html>
 *
 * For additional Tracker information and documentation, please see
 * <http://physlets.org/tracker/>.
 */
package org.opensourcephysics.cabrillo.tracker;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.media.core.VideoPlayer;
import org.opensourcephysics.tools.FontSizer;

/**
 * Dialog that selects and configures the {@link MotionFilter} applied to point mass
 * positions before velocity and acceleration are computed. The same dialog can be
 * applied to a single point mass or to all point masses in the TrackerPanel.
 *
 * @author Tracker Filter contribution
 */
@SuppressWarnings("serial")
public class MotionFilterDialog extends JDialog {

	private static final String CARD_NONE = "none"; //$NON-NLS-1$
	private static final String CARD_MA = "ma"; //$NON-NLS-1$
	private static final String CARD_BUTTER = "butter"; //$NON-NLS-1$
	private static final String CARD_SG = "sg"; //$NON-NLS-1$

	protected TFrame frame;
	protected Integer panelID;

	protected ArrayList<PointMass> targetMasses = new ArrayList<PointMass>();

	private JRadioButton noneButton, maButton, butterButton, sgButton;
	private TitledBorder choiceBorder;
	private JTextPane infoPane;

	private JPanel cards;
	private CardLayout cardLayout;

	private JSpinner maWindowSpinner;
	private JSpinner butterOrderSpinner, butterCutoffSpinner;
	private JLabel butterRateLabel;
	private JSpinner sgWindowSpinner, sgPolySpinner;

	private JButton okButton, cancelButton;

	private MotionFilter prevFilter;
	private boolean updating;

	public MotionFilterDialog(TrackerPanel panel) {
		super(panel.getTFrame(), true);
		frame = panel.getTFrame();
		panelID = panel.getID();
		createGUI();
		pack();
		okButton.requestFocusInWindow();
	}

	protected void setTargetMass(PointMass mass) {
		targetMasses.clear();
		targetMasses.add(mass);
		refreshGUI();
	}

	protected void setTargetMasses(ArrayList<PointMass> masses) {
		targetMasses.clear();
		targetMasses.addAll(masses);
		refreshGUI();
	}

	private void createGUI() {
		JPanel contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);

		Box choicebar = Box.createHorizontalBox();
		choiceBorder = BorderFactory.createTitledBorder(""); //$NON-NLS-1$
		Border empty = BorderFactory.createEmptyBorder(3, 2, 3, 2);
		choicebar.setBorder(BorderFactory.createCompoundBorder(empty, choiceBorder));
		ButtonGroup group = new ButtonGroup();

		Action chooser = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (updating) return;
				String card = e.getActionCommand();
				cardLayout.show(cards, card);
				applyCurrent();
				refreshInfo();
			}
		};

		noneButton = makeRadio(group, choicebar, chooser, CARD_NONE);
		maButton = makeRadio(group, choicebar, chooser, CARD_MA);
		butterButton = makeRadio(group, choicebar, chooser, CARD_BUTTER);
		sgButton = makeRadio(group, choicebar, chooser, CARD_SG);

		contentPane.add(choicebar, BorderLayout.NORTH);

		cardLayout = new CardLayout();
		cards = new JPanel(cardLayout);
		cards.add(buildNoneCard(), CARD_NONE);
		cards.add(buildMovingAverageCard(), CARD_MA);
		cards.add(buildButterworthCard(), CARD_BUTTER);
		cards.add(buildSavitzkyGolayCard(), CARD_SG);

		infoPane = new JTextPane();
		infoPane.setEditable(false);
		infoPane.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		JScrollPane infoScroll = new JScrollPane(infoPane);

		JPanel center = new JPanel(new BorderLayout());
		center.add(cards, BorderLayout.NORTH);
		center.add(infoScroll, BorderLayout.CENTER);
		contentPane.add(center, BorderLayout.CENTER);

		okButton = new JButton();
		okButton.setForeground(new Color(0, 0, 102));
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		cancelButton = new JButton();
		cancelButton.setForeground(new Color(0, 0, 102));
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				revert();
				setVisible(false);
			}
		});
		JPanel buttonbar = new JPanel();
		buttonbar.setBorder(BorderFactory.createEmptyBorder(1, 0, 3, 0));
		buttonbar.add(okButton);
		buttonbar.add(cancelButton);
		contentPane.add(buttonbar, BorderLayout.SOUTH);

		refreshGUI();
	}

	private JRadioButton makeRadio(ButtonGroup g, Box bar, Action a, String cmd) {
		JRadioButton b = new JRadioButton();
		b.setActionCommand(cmd);
		b.addActionListener(a);
		g.add(b);
		bar.add(b);
		return b;
	}

	private JPanel buildNoneCard() {
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		return p;
	}

	private JPanel buildMovingAverageCard() {
		JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
		p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		maWindowSpinner = new JSpinner(new SpinnerNumberModel(5, 3, 99, 2));
		maWindowSpinner.addChangeListener(applyOnChange());
		p.add(new JLabel(TrackerRes.getString("FilterDialog.Param.Window"))); //$NON-NLS-1$
		p.add(maWindowSpinner);
		return p;
	}

	private JPanel buildButterworthCard() {
		JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
		p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		butterOrderSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 8, 1));
		butterOrderSpinner.addChangeListener(applyOnChange());
		butterCutoffSpinner = new JSpinner(new SpinnerNumberModel(6.0, 0.1, 1000.0, 0.5));
		butterCutoffSpinner.addChangeListener(applyOnChange());
		butterRateLabel = new JLabel("--"); //$NON-NLS-1$
		p.add(new JLabel(TrackerRes.getString("FilterDialog.Param.Order"))); //$NON-NLS-1$
		p.add(butterOrderSpinner);
		p.add(new JLabel(TrackerRes.getString("FilterDialog.Param.Cutoff"))); //$NON-NLS-1$
		p.add(butterCutoffSpinner);
		p.add(new JLabel(TrackerRes.getString("FilterDialog.Param.SampleRate"))); //$NON-NLS-1$
		p.add(butterRateLabel);
		return p;
	}

	private JPanel buildSavitzkyGolayCard() {
		JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
		p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		sgWindowSpinner = new JSpinner(new SpinnerNumberModel(7, 5, 99, 2));
		sgWindowSpinner.addChangeListener(applyOnChange());
		sgPolySpinner = new JSpinner(new SpinnerNumberModel(2, 1, 6, 1));
		sgPolySpinner.addChangeListener(applyOnChange());
		p.add(new JLabel(TrackerRes.getString("FilterDialog.Param.Window"))); //$NON-NLS-1$
		p.add(sgWindowSpinner);
		p.add(new JLabel(TrackerRes.getString("FilterDialog.Param.PolyOrder"))); //$NON-NLS-1$
		p.add(sgPolySpinner);
		return p;
	}

	private ChangeListener applyOnChange() {
		return new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (updating) return;
				applyCurrent();
				refreshInfo();
			}
		};
	}

	private double estimateSampleRateHz() {
		try {
			TrackerPanel panel = frame.getTrackerPanelForID(panelID);
			VideoPlayer player = panel.getPlayer();
			double meanMs = player.getMeanStepDuration();
			if (meanMs > 0) return 1000.0 / meanMs;
		} catch (Exception ignored) {}
		return 30.0;
	}

	private void applyCurrent() {
		if (targetMasses.isEmpty()) return;
		MotionFilter f = buildFilterFromUI();
		for (PointMass m : targetMasses) {
			m.setMotionFilter(f == null ? null : f.copy());
		}
	}

	private MotionFilter buildFilterFromUI() {
		if (maButton.isSelected()) {
			int w = (Integer) maWindowSpinner.getValue();
			return new MovingAverageFilter(w);
		} else if (butterButton.isSelected()) {
			int order = (Integer) butterOrderSpinner.getValue();
			double cutoff = ((Number) butterCutoffSpinner.getValue()).doubleValue();
			double fs = estimateSampleRateHz();
			return new ButterworthFilter(order, cutoff, fs);
		} else if (sgButton.isSelected()) {
			int w = (Integer) sgWindowSpinner.getValue();
			int p = (Integer) sgPolySpinner.getValue();
			return new SavitzkyGolayFilter(w, p);
		}
		return null;
	}

	private void refreshGUI() {
		String target = targetMasses.size() == 1 ? targetMasses.get(0).getName()
				: TrackerRes.getString("AlgorithmDialog.TargetMasses.All"); //$NON-NLS-1$
		setTitle(TrackerRes.getString("FilterDialog.Title") + ": " + target); //$NON-NLS-1$ //$NON-NLS-2$
		choiceBorder.setTitle(TrackerRes.getString("FilterDialog.TitledBorder.Choose")); //$NON-NLS-1$
		okButton.setText(TrackerRes.getString("Dialog.Button.OK")); //$NON-NLS-1$
		cancelButton.setText(TrackerRes.getString("Dialog.Button.Cancel")); //$NON-NLS-1$
		noneButton.setText(TrackerRes.getString("FilterDialog.None.Name")); //$NON-NLS-1$
		maButton.setText(TrackerRes.getString("FilterDialog.MovingAverage.Name")); //$NON-NLS-1$
		butterButton.setText(TrackerRes.getString("FilterDialog.Butterworth.Name")); //$NON-NLS-1$
		sgButton.setText(TrackerRes.getString("FilterDialog.SavitzkyGolay.Name")); //$NON-NLS-1$
		if (butterRateLabel != null) {
			butterRateLabel.setText(String.format("%.2f Hz", estimateSampleRateHz())); //$NON-NLS-1$
		}
	}

	private void refreshInfo() {
		String s;
		if (noneButton.isSelected()) {
			s = TrackerRes.getString("FilterDialog.None.Description"); //$NON-NLS-1$
		} else if (maButton.isSelected()) {
			s = TrackerRes.getString("FilterDialog.MovingAverage.Description"); //$NON-NLS-1$
		} else if (butterButton.isSelected()) {
			s = TrackerRes.getString("FilterDialog.Butterworth.Description"); //$NON-NLS-1$
		} else if (sgButton.isSelected()) {
			s = TrackerRes.getString("FilterDialog.SavitzkyGolay.Description"); //$NON-NLS-1$
		} else {
			s = ""; //$NON-NLS-1$
		}
		infoPane.setText(s);
	}

	private void initialize() {
		updating = true;
		try {
			MotionFilter current = targetMasses.isEmpty() ? null : targetMasses.get(0).getMotionFilter();
			prevFilter = current == null ? null : current.copy();
			selectCardForFilter(current);
		} finally {
			updating = false;
		}
		refreshInfo();
	}

	private void selectCardForFilter(MotionFilter f) {
		if (f == null) {
			noneButton.setSelected(true);
			cardLayout.show(cards, CARD_NONE);
			return;
		}
		if (f instanceof MovingAverageFilter) {
			MovingAverageFilter ma = (MovingAverageFilter) f;
			maWindowSpinner.setValue(ma.getWindow());
			maButton.setSelected(true);
			cardLayout.show(cards, CARD_MA);
		} else if (f instanceof ButterworthFilter) {
			ButterworthFilter bw = (ButterworthFilter) f;
			butterOrderSpinner.setValue(bw.getOrder());
			butterCutoffSpinner.setValue(bw.getCutoffHz());
			if (butterRateLabel != null)
				butterRateLabel.setText(String.format("%.2f Hz", bw.getSampleRateHz())); //$NON-NLS-1$
			butterButton.setSelected(true);
			cardLayout.show(cards, CARD_BUTTER);
		} else if (f instanceof SavitzkyGolayFilter) {
			SavitzkyGolayFilter sg = (SavitzkyGolayFilter) f;
			sgWindowSpinner.setValue(sg.getWindow());
			sgPolySpinner.setValue(sg.getPolyOrder());
			sgButton.setSelected(true);
			cardLayout.show(cards, CARD_SG);
		}
	}

	private void revert() {
		for (PointMass m : targetMasses) {
			m.setMotionFilter(prevFilter == null ? null : prevFilter.copy());
		}
	}

	@Override
	public void setVisible(boolean vis) {
		initialize();
		super.setVisible(vis);
	}

	protected void setFontLevel(int level) {
		FontSizer.setFonts(this, level);
		FontSizer.setFonts(choiceBorder, level);
		int w = (int) (480 * (1 + level * .35));
		int h = (int) (160 * (1 + level * .35));
		infoPane.setPreferredSize(new Dimension(w, h));
		pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (dim.width - getBounds().width) / 2;
		int y = (dim.height - getBounds().height) / 2;
		setLocation(x, y);
	}

	@Override
	public void dispose() {
		panelID = null;
		frame = null;
		super.dispose();
	}

	@SuppressWarnings("unused")
	private static Component placeholder() {
		return new JPanel();
	}

}
