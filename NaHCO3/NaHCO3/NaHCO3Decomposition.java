package NaHCO3;


import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import lab.util.animation.DoubleLerpAnimation;
import lab.util.animation.DoubleLinearAnimation;
import lab.util.animation.IntegerLinearAnimation;
import lab.LabFrame;
import lab.component.BunsenBurner;
import lab.component.EmptyComponent;
import lab.component.MeasurableComponent;
import lab.component.container.Bulb;
import lab.component.container.ContentState;
import lab.component.fx.ParticleShape;
import lab.component.fx.ParticleSystem;
import lab.component.fx.RandomVector2Generator;
import lab.component.sensor.Manometer;
import lab.component.sensor.Thermometer;
import lab.component.swing.Label;
import lab.component.swing.input.Button;
import lab.util.SigFig;
import lab.util.Vector2;
import lab.util.VerticalGraduation;

public class NaHCO3Decomposition extends LabFrame {

	public static void main(String[] args) {
		new NaHCO3Decomposition("Heterogeneous Equilibrium: Decomposition of Sodium Bicarbonate", 10.0, 0.5, 0.559,
				0.00851, 20, 800);
	}

	private static final long serialVersionUID = 1L;
	private final double Kp;
	private final double initialTemp;
	private final double temp;

	private final Manometer manometer;
	private final Bulb bulb;
	private final Thermometer thermometer;
	private final ParticleSystem gasParticles;
	private final BunsenBurner burner;

	private final Button resetButton;
	private final Button addSubstanceButton;
	private final Button evacuateButton;
	private final Button heatButton;
	private final Button detailsButton;

	private final LabFrame detailsWindow;

	private boolean reactionOccuring = false;

	public NaHCO3Decomposition(String name, double mass, double volume, double Kp, double Kc, double initialTemp,
			double temp) {
		super(name, 660, 725);

		this.Kp = Kp;
		this.initialTemp = initialTemp;
		this.temp = temp;

		manometer = new Manometer(150, 600);
		manometer.setOffsetY(20);
		manometer.setGraduation(new VerticalGraduation(0, 760, 20, 5));
		manometer.setValue(760.0);

		EmptyComponent middleContentArea = new EmptyComponent(300, 600);
		middleContentArea.setOffsetX(20);

		bulb = new Bulb(300, 300);
		bulb.setOffsetX(35);
		bulb.setOffsetY(60);
		bulb.setContentColor(new Color(240, 240, 240));
		bulb.setContentState(ContentState.SOLID);

		burner = new BunsenBurner(20, 175);
		burner.setOffsetY(1);
		burner.setOffsetX(175);
		burner.getFlame().setVisible(false);
		burner.getFlame().setIntensity(0);

		gasParticles = new ParticleSystem(300, 300, 50);
		gasParticles.setLifetime(Integer.MAX_VALUE);
		gasParticles.setParticleSpawnRate(Double.MAX_VALUE);
		gasParticles.setSpawnArea(new Vector2(150, 295));
		gasParticles.setColor(Color.black);
		gasParticles.setColorFade(0);
		gasParticles.setShape(ParticleShape.ELLIPSE);
		gasParticles.setParticleWidth(6);
		gasParticles.setParticleHeight(6);
		gasParticles.setParticleWidthChange(0);
		gasParticles.setParticleHeightChange(0);
		gasParticles.setVelocity(new RandomVector2Generator(6));
		gasParticles.setColor(new Color(0, 0, 255));

		for (int i = 1; i < Bulb.POLY1_X.length - 3; i++) {
			gasParticles.addCollidableEdge(Bulb.POLY1_X[i - 1] * bulb.getWidth(),
					Bulb.POLY1_Y[i - 1] * bulb.getHeight(), Bulb.POLY1_X[i] * bulb.getWidth(),
					Bulb.POLY1_Y[i] * bulb.getHeight());
			gasParticles.addCollidableEdge(Bulb.POLY2_X[i - 1] * bulb.getWidth(),
					Bulb.POLY2_Y[i - 1] * bulb.getHeight(), Bulb.POLY2_X[i] * bulb.getWidth(),
					Bulb.POLY2_Y[i] * bulb.getHeight());
		}

		gasParticles.addCollidableEdge(Bulb.POLY2_X[0] * bulb.getWidth(), Bulb.POLY2_Y[0] * bulb.getHeight(),
				Bulb.POLY1_X[0] * bulb.getWidth(), Bulb.POLY1_Y[0] * bulb.getHeight());

		gasParticles.start();

		bulb.addChild(gasParticles);

		middleContentArea.addChild(bulb, burner);

		thermometer = new Thermometer(500);
		thermometer.setOffsetX(80);
		thermometer.setOffsetY(20);
		thermometer.setGraduation(new VerticalGraduation(0, 1000, 100, 20));
		thermometer.setValue(initialTemp);
		thermometer.getGraduation().setSuffix("C");

		addComponent(manometer, middleContentArea, thermometer);

		resetButton = new Button(150, 25, "Reset Experiment") {
			@Override
			public void doSomething() {
				resetExperiment();
			}
		};

		resetButton.setOffsetX(30);

		addSubstanceButton = new Button(150, 25, "Add NaHCO3") {
			@Override
			public void doSomething() {
				addSubstance();
			}
		};

		evacuateButton = new Button(150, 25, "Evacuate Bulb") {
			@Override
			public void doSomething() {
				evacuate();
			}
		};

		heatButton = new Button(150, 25, "Heat System") {
			@Override
			public void doSomething() {
				heat();
			}
		};

		detailsButton = new Button(150, 25, "Show Details") {
			@Override
			public void doSomething() {
				detailsWindow.setVisible(true);
			}
		};

		detailsButton.setOffset(30, 5);

		Label reactionLabel = new Label(250, 15, "NaHCO3(s) <=> NaOH(s) + CO2(g)");
		reactionLabel.setOffsetY(0);

		resetButton.setOffsetY(5);
		addSubstanceButton.setOffsetY(5);
		addSubstanceButton.setOffsetY(5);
		evacuateButton.setOffsetY(5);
		heatButton.setOffsetY(5);

		addComponent(new EmptyComponent(250, 1), reactionLabel, resetButton, addSubstanceButton, evacuateButton,
				heatButton, detailsButton);

		detailsWindow = new LabFrame("Simulation Details", 400, 250, false) {

			private static final long serialVersionUID = 1L;

			@Override
			public void update() {

			}
		};

		detailsWindow.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		detailsWindow.setResizable(false);

		Label massLabel, volumeLabel, atmPressureLabel, initTempLabel, finalTempLabel, KpLabel, KcLabel,
				questionMarkLabel;

		massLabel = new Label(400, 30, "Sodium Bicarbonate Mass = " + mass + "g");
		massLabel.setFontSize(20);
		massLabel.setOffset(10, 0);

		volumeLabel = new Label(400, 30, "Bulb Volume = " + SigFig.sigfigalize(volume, 3) + "L");
		volumeLabel.setFontSize(20);
		volumeLabel.setOffset(10, 0);

		atmPressureLabel = new Label(400, 30, "Atmosphere Pressure = 1.0 atm");
		atmPressureLabel.setFontSize(20);
		atmPressureLabel.setOffset(10, 0);

		initTempLabel = new Label(400, 30, "Initial Temperature = " + SigFig.sigfigalize(initialTemp, 3, 4) + "C");
		initTempLabel.setFontSize(20);
		initTempLabel.setOffset(10, 0);

		finalTempLabel = new Label(400, 30, "Final Temperature = " + SigFig.sigfigalize(temp, 3, 4) + "C");
		finalTempLabel.setFontSize(20);
		finalTempLabel.setOffset(10, 0);

		KpLabel = new Label(400, 30, "Kp = " + Kp);
		KpLabel.setFontSize(20);
		KpLabel.setOffset(10, 0);

		KcLabel = new Label(55, 30, "Kc = ");
		KcLabel.setFontSize(20);
		KcLabel.setOffset(10, 0);

		questionMarkLabel = new Label(300, 30, "   ?   ");

		// this is bad...
		Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
		fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		Font underline = new Font(Font.SANS_SERIF, Font.PLAIN, 20).deriveFont(fontAttributes);

		questionMarkLabel.setFont(underline);
		questionMarkLabel.setFontSize(20);
		questionMarkLabel.setOffset(0, 0);

		detailsWindow.addComponent(massLabel, volumeLabel, atmPressureLabel, initTempLabel, finalTempLabel, KpLabel,
				KcLabel, questionMarkLabel);
		detailsWindow.start(0);

		resetExperiment();

		start(30);
	}

	private void animateMeasurable(double value, final MeasurableComponent c) {

		getAnimator().addAnimation(c.toString(), new DoubleLerpAnimation(value, 0.05f, 1.0) {
			@Override
			public Double getValue() {
				return c.getValue();
			}

			@Override
			public void setValue(Double v) {
				c.setValue(v);
			}
		});
	}

	public void resetExperiment() {
		animateMeasurable(760, manometer);
		animateMeasurable(initialTemp, thermometer);

		if (getAnimator().animationExists("removeSolid")) {
			getAnimator().getAnimation("removeSolid").cancel();
		}

		bulb.setValue(0.0);
		addSubstanceButton.setEnabled(true);
		evacuateButton.setEnabled(false);
		heatButton.setEnabled(false);
		reactionOccuring = false;
		gasParticles.stop();
		gasParticles.setParticleSpawnRate(Double.MAX_VALUE);

		burner.getFlame().setVisible(false);
		burner.getFlame().setIntensity(0);
	}

	public void addSubstance() {
		bulb.setValue(20.0);
		evacuateButton.setEnabled(true);
		addSubstanceButton.setEnabled(false);
		gasParticles.start();
		gasParticles.spawnParticle();
	}

	public void evacuate() {
		animateMeasurable(0, manometer);

		gasParticles.stop();
		gasParticles.start();
		gasParticles.spawnParticle();

		evacuateButton.setEnabled(false);
		heatButton.setEnabled(true);

	}

	public void heat() {
		animateMeasurable(temp, thermometer);

		heatButton.setEnabled(false);

		gasParticles.spawnParticle();

		reactionOccuring = true;

		burner.getFlame().setVisible(true);

		getAnimator().addAnimation("flame", new IntegerLinearAnimation(150, 5) {
			@Override
			public Integer getValue() {
				return burner.getFlame().getIntensity();
			}

			@Override
			public void setValue(Integer v) {
				burner.getFlame().setIntensity(v);
			}
		});

		getAnimator().addAnimation("removeSolid", new DoubleLinearAnimation(15.0, 0.06) {
			@Override
			public Double getValue() {
				return bulb.getValue();
			}

			@Override
			public void setValue(Double v) {
				bulb.setValue(v);
			}
		});

	}

	@Override
	public void update() {

		if (reactionOccuring) {
			gasParticles.setParticleSpawnRate(105 - thermometer.getValue() / temp * 100.0);

			double p = ((double) gasParticles.getActiveParticles() / gasParticles.getTotalParticles()) * Kp * 760.0;

			animateMeasurable(p, manometer);
		}

	}

}
