/**
 * PROJECT   : Elbfisch - java process automation controller (jPac)
 * MODULE    : RevPi.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : 
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 *
 * This file is part of the jPac process automation controller.
 * jPac is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jPac is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the jPac If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpac.vioss.revpi;

/**
 *
 * @author berndschuster
 */
public class RevPi {
        //Led A1,A2 control
        public enum CoreV12 { 
            LedA1Off    (0x00),
            LedA1Green  (0x01),
            LedA1Red    (0x02),
            LedA1Orange (0x03),
            LedA2Off    (0x00),
            LedA2Green  (0x04),
            LedA2Red    (0x08),
            LedA2Orange (0x0C);
        
            public final int value;

            CoreV12(int value){
                this.value = value;
            }
        }

        public enum Status { 
            Running                  (0x01),
            ModuleNotConfigured      (0x02),
            ModuleNotMounted         (0x04),
            ModuleConfigurationError (0x08),
            PiGateMountedOnLeftSide  (0x10),
            PiGateMountedOnRightSide (0x20);
        
            public final int value;

            Status(int value){
                this.value = value;
            }            
        }
        static public class DIO{
            public enum InputMode {
                Direct             (0x00),
                CounterRisingEdge  (0x01),
                CounterFallingEdge (0x02),
                Encoder            (0x03);
                
                public final int value;

                InputMode(int value){
                    this.value = value;
                }
            }
            public enum InputDebounce {
                Off       (0x00),
                Micros25  (0x01),
                Micros750 (0x02),
                Millis3   (0x03);

                public final int value;

                InputDebounce(int value){
                    this.value = value;
                }
            }
            public enum OutputPWMFrequency {
                Hz40   (0x00),
                Hz80   (0x01),
                Hz160  (0x02),
                Hz200  (0x03),
                Hz400  (0x04);

                public final int value;

                OutputPWMFrequency(int value){
                    this.value = value;
                }
            }            
            public enum Status { 
                AccessError              (0x0001),
                UnderVoltage1Input1_8    (0x0002),
                UnderVoltage2Input1_8    (0x0004),
                OverTemperatureInput1_8  (0x0008),
                UnderVoltage1Input9_16   (0x0010),
                UnderVoltage2Input9_16   (0x0020),
                OverTemperatureInput9_16 (0x0040),
                GeneralFailure           (0x0080),
                CommunicationError       (0x0100),
                CRCError                 (0x0200);

                public final int value;

                Status(int value){
                    this.value = value;
                }
            }
        }
        
        static public class AIO{
            public enum InputRange{
                VoltageM10To10V     (0),//-10 V - +10 V
                Voltage0To10V       (1),//  0 V - +10 V
                Voltage0To5V        (2),//  0 V -  +5 V  
                VoltageM5To5V       (3),// -5 V -  +5 V
                Current0To20mA      (4),//  0 mA - 20 mA
                Current0To24mA      (5),//  0 mA - 24 mA
                Current4To20mA      (6),//  0 mA - 24 mA
                CurrentM25ToP25mA   (7);//-25 mA - 25 mA
                
                public final int value;
                
                InputRange(int value){
                    this.value = value;
                }
            }
            public enum AdcDataRate{
                Hz5              (0),//  5  Hz
                Hz10             (1),//  10  Hz
                Hz20             (2),//  20  Hz  
                Hz40             (3),//  40  Hz
                Hz80             (4),//  80  Hz
                Hz160            (5),//  160 Hz
                Hz320            (6),//  320 Hz
                Hz640            (7);//  640 Hz
                
                public final int value;
                
                AdcDataRate(int value){
                    this.value = value;
                }
            }
            public enum Status { 
                Running                  (0x01),
                ModuleNotConfigured      (0x02),
                ModuleNotMounted         (0x04),
                ModuleConfigurationError (0x08),
                PiGateMountedOnLeftSide  (0x10),
                PiGateMountedOnRightSide (0x20);

                public final int value;

                Status(int value){
                    this.value = value;
                }
        }
    }
}
        
