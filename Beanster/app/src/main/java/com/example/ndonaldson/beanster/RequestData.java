package com.example.ndonaldson.beanster;

/**
 * Created by ndonaldson on 10/2/18.
 */

/**
 • Brew water temperature; as a value in degress Farenheit.
 • Frothing pressure; as a value in PSI.
 • Brew water pressure, as a value in PSI.
 • Amount of water to dispense through the brewer, as a value in ounces.
 • Amount of milk dispensed through the frother, in ounces.
 • Temprature for milk to reach in frothing cycle, as a value in degrees Fahrenheit.
 • Amount of froth to produce; as a value from zero to 100.
 A value of zero will cause the milk to simply be warmed up (steamed), while a higher value will produce more foam.
 • What kind of syrup, along with how much; as two integer values. The first number will be a value between zero and four.
 The second number will be the amount of syrup to dispense; in ounces.
 • The amount of coffee to dispense; in kilograms.
 */
public class RequestData{
    private int waterTemp;
    private int milkTemp;
    private int waterPress;
    private int frothPress;
    private int waterDisp;
    private int milkDisp;
    private int frothDisp;
    private int coffeeDisp;
    private int syrupDisp;
    private int syrup;

    public RequestData(){
        this.waterTemp = 70;
        this.milkTemp = 70;
        this.waterPress = 70;
        this.frothPress = 70;
        this.waterDisp = 70;
        this.milkDisp = 70;
        this.frothDisp = 70;
        this.coffeeDisp = 70;
        this.syrupDisp = 70;
        this.syrup = 70;
    }


    //TODO: NEED DEFAULT VALUES
    public void setWithBasic(CoffeeBrew.BasicState basicState){
        switch (basicState.amount){
            case FIRST:{
                break;
            }
            case SECOND:{
                break;
            }
            case THIRD:{
                break;
            }
        }

        switch (basicState.froth){
            case FIRST:{
                break;
            }
            case SECOND:{
                break;
            }
            case THIRD:{
                break;
            }
        }

        switch (basicState.strength){
            case FIRST:{
                break;
            }
            case SECOND:{
                break;
            }
            case THIRD:{
                break;
            }
        }
    }

    public void setWithAdvance(CoffeeBrew.AdvancedState advancedState){
        this.waterTemp = advancedState.waterState.temp;
        this.waterPress = advancedState.waterState.press;
        this.waterDisp = advancedState.waterState.disp;
        this.milkTemp = advancedState.milkState.temp;
        this.milkDisp = advancedState.milkState.disp;
        this.frothPress = advancedState.frothState.press;
        this.frothDisp = advancedState.frothState.disp;
        this.coffeeDisp = advancedState.coffeeState.disp;
        this.syrupDisp = advancedState.syrupState.disp;
        this.syrup = advancedState.syrupState.type;
    }

    @Override
    public boolean equals(Object o){

        if(o == this) return true;
        if(!(o instanceof RequestData)) return false;

        RequestData r = (RequestData) o;

        if(this.waterDisp == r.waterDisp && this.waterPress == r.waterPress && this.waterTemp == r.waterTemp &&
                this.milkDisp == r.milkDisp && this.milkTemp == r.milkTemp && this.frothDisp == r.frothDisp &&
                this.frothPress == r.frothPress && this.coffeeDisp == r.coffeeDisp &&
                this.syrupDisp == r.syrupDisp && this.syrup == r.syrup)
            return true;

        return false;
    }
}
