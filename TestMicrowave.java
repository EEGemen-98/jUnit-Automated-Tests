/**
 * 
 */
package microwave;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

import javax.sound.midi.SysexMessage;

/**
 * @author whalen
 *
 */
@RunWith(JUnitQuickcheck.class)
public class TestMicrowave {

	public Microwave microwave;
	
	@Before
	public void setUp() {
		microwave = new Microwave(new ModeController(), new DisplayController(1), new ArrayList<Preset>());
	}

	public String digitsString(byte [] digits) {
    	return "" + 
    			digits[DisplayController.TENS_OF_MINUTES] +  
    			digits[DisplayController.MINUTES] + 
    			digits[DisplayController.TENS_OF_SECONDS] +  
    			digits[DisplayController.SECONDS];
	}

	/** seconds_elapse
	 * 
	 * This method allows 'time' seconds to elapse with the microwave.
	 * 
	 * @param time
	 */
    public void secondsElapse(int time)  {
        // System.out.println("" + time + " seconds elapse.");
        for (int i = 0; i < time*microwave.getTickRateInHz(); i++) {
        	microwave.tick(); 
        }
    }

    public int calcTime(byte [] digits) {
    	return digits[DisplayController.TENS_OF_MINUTES] * 600 + 
    		   digits[DisplayController.MINUTES] * 60 + 
    		   digits[DisplayController.TENS_OF_SECONDS] * 10 + 
    		   digits[DisplayController.SECONDS];
    }

	public String printDigits(byte[] digits) {
		return "" + digits[0] + digits[1] + digits[2] + digits[3];
	}

    public boolean timeIsNormal() {
    	byte[] digits = microwave.digits();
    	return (digits[DisplayController.TENS_OF_MINUTES] <= 5 && 
    			digits[DisplayController.MINUTES] <= 9 &&
    			digits[DisplayController.TENS_OF_SECONDS] <= 5 &&
    			digits[DisplayController.SECONDS] <= 9);
    }
        
    public boolean timeIsOk() {
    	byte[] digits = microwave.digits();
    	for (byte digit: digits) {
    		if (digit < 0 || digit > 9) {
    			return false;
    		}
    	}
    	return true;
    }

	public boolean secondsElapseNormal(int time)  {
		// System.out.println("" + time + " seconds elapse.");
		boolean prevNormal, currNormal;
		prevNormal = timeIsNormal();

		for (int i = 0; i < time*microwave.getTickRateInHz(); i++) {
			microwave.tick();
			assertTrue(timeIsOk());
			currNormal = timeIsNormal();
			if (prevNormal)
				assertTrue(currNormal);
			prevNormal = currNormal;
		}
		return true;
	}

	/**
	 * The following parameterized unit tests are written by Egemen Bozkus
	 */

	/*
	 *  Create a parameterized unit test for the 'Cooking' User Story.
	 *  
	 *  This test takes as arguments four
	 *  digits that represent the user's input for cooking time, and 
	 *  an argument for the time elapsed.  Then let the microwave cook for
	 *  that amount of time.  Note that you need to tick() after pressing 
	 *  each button for it to register with the microwave.
	 *  
	 *  If the time elapsed is greater or equal to the time to cook, then 
	 *  the time remaining should be zero.  Otherwise, the time remaining 
	 *  should be the input time minus the time elapsed.  Check that the 
	 *  time remaining is correctly computed by the model.
	 *  
	 *  In addition, if the time to cook is zero, then the mode should be 
	 *  setup; otherwise it should be cooking.  Check that the microwave
	 *  is in the correct mode.
	 *  
	 *  Note: Remember to pay attention to ranges on inputs!  Not just 
 	 *  the digits, but the time to cook; if you allow it to be too 
	 *  big, your tests may run too long; too small, and they may 
	 *  not do anything interesting, or do the wrong thing.
	 *  
	 */

	@Property(trials = 100)
	public void checkCookingTimes(@InRange(min = "0", max = "9") int digit1, @InRange(min = "0", max = "9")  int digit2,
								   @InRange(min = "0", max = "9")  int digit3, @InRange(min = "0", max = "9") int digit4,
								   @InRange(min = "0", max = "9999") int timeToCook) {
		microwave.setDoorOpen(false);
		microwave.tick();
		microwave.digitPressed(digit1);
		microwave.tick();
		microwave.digitPressed(digit2);
		microwave.tick();
		microwave.digitPressed(digit3);
		microwave.tick();
		microwave.digitPressed(digit4);
		microwave.tick();
		microwave.startPressed();
		microwave.tick();


		int timeEnteredInSeconds = digit1 * 600 + digit2 * 60 + digit3 * 10 + digit4;
		System.out.println("Time entered (seconds): " + timeEnteredInSeconds);

		this.secondsElapse(timeToCook);
		byte [] digits = microwave.digits();
		int timeRemain = calcTime(digits);

		ModeController.Mode mode = microwave.getMode();

		System.out.println("Time to cook: " + timeToCook + "; ");

		boolean timeIsCorrectlyDisplayed;
		boolean modeIsCorrect;

		if (timeToCook < timeEnteredInSeconds) {
			System.out.println("Not enough time to finish cooking. This: " + timeRemain + " should equal: " +
					(timeEnteredInSeconds - timeToCook));
			timeIsCorrectlyDisplayed = timeRemain == timeEnteredInSeconds - timeToCook;
			modeIsCorrect = mode == ModeController.Mode.Cooking;
		}
		else {
			System.out.println("Finished cooking, time should be 0 check: " + timeRemain);
			timeIsCorrectlyDisplayed = timeRemain == 0;
			modeIsCorrect = mode == ModeController.Mode.Setup;
		}
		System.out.println();
		assertTrue(timeIsCorrectlyDisplayed);
		assertTrue(modeIsCorrect);
	}

	
	/*
	 * Create a parameterized unit test for "User presses preset button".  The parameter
	 * to the test should be the preset.  The goal is to check that after the preset is 
	 * pressed, the time to cook and power level matches the chosen preset.  If the 
	 * input preset is out of range, then the test should check that no change to the 
	 * system is made.
	 * 
	 * One thing to keep in mind: don't make the range of presets the full range of 
	 * integers, or you will almost never get a valid preset (what is a good range?).
	 * 
	 * Also: from the UI, presets start at 1 not 0.  So, you need to convert when accessing
	 * the presets array.  The microwave.presetPressed(preset) function indexes from 1 not 0.
	 * 
	 */
	@Property(trials = 100)
	public void checkPreset(@InRange(min = "0", max = "9") int preset) {
		ArrayList<Preset> presetList = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			presetList.add(new Preset("Preset" + (i + 1), 10 * (i + 1), (i + 1)));
		}
		if (preset > presetList.size() || preset < 1) {return;}

		microwave = new Microwave(new ModeController(), new DisplayController(1), presetList);
		microwave.presetPressed(preset);
		microwave.tick();

		Preset presetActual = presetList.get(preset - 1);
		byte[] digits = microwave.digits();
		assertEquals(calcTime(digits), presetActual.getTimeToCook());
		assertEquals(microwave.getPowerLevel(), presetActual.getPowerLevel());
		
	}
	
	/* 
	 * Create a parameterized unit test that can check "unusual times", like 1 minute, 99 seconds
	 * The test should check that if the test reaches a "normal time" 
	 * (where tens-of-seconds and tens-of-minutes are both <= 5, and seconds and 
	 * minutes are both <= 9) then thereafter, it stays in "normal times".  
	 * In all cases the test should count down the correct number of seconds.  Also, in 
	 * all cases, all digits should be between 0..9.
	 * 
	 * To determine "normal times", you may need to rewrite or replace "secondsElapse" to 
	 * determine whether a time is "normal" 
	 */
	@Property(trials=100)
	public void checkUnusualCookingTimes(@InRange(min = "0", max = "9") int digit1, @InRange(min = "0", max = "9")  int digit2,
										 @InRange(min = "0", max = "9")  int digit3, @InRange(min = "0", max = "9") int digit4,
										 @InRange(min = "0", max = "9999") int timeToCook) {
		microwave.setDoorOpen(false);
		microwave.tick();
		microwave.digitPressed(digit1);
		microwave.tick();
		microwave.digitPressed(digit2);
		microwave.tick();
		microwave.digitPressed(digit3);
		microwave.tick();
		microwave.digitPressed(digit4);
		microwave.tick();
		microwave.startPressed();
		microwave.tick();

		this.secondsElapseNormal(timeToCook);
		byte [] digits = microwave.digits();

		int timeEnteredInSeconds = digit1 * 600 + digit2 * 60 + digit3 * 10 + digit4;
		int timeRemain = digits[0]*600 + digits[1]*60 + digits[2]*10 + digits[3];

		ModeController.Mode mode = microwave.getMode();
		boolean modeIsCorrect;

		if (timeToCook < timeEnteredInSeconds)
			modeIsCorrect = mode == ModeController.Mode.Cooking;
		else
			modeIsCorrect = mode == ModeController.Mode.Setup;

		assertEquals(Integer.max(timeEnteredInSeconds - timeToCook, 0), timeRemain);
		assertTrue(modeIsCorrect);
	}
}
