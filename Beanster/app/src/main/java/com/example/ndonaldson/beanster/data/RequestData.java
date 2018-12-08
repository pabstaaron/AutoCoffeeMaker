package com.example.ndonaldson.beanster.data;

/**
 * Created by ndonaldson on 10/2/18.
 */

import com.example.ndonaldson.beanster.activities.CoffeeBrew;

/**
 • Brew water temperature; as a value in degress Farenheit.
 • Amount of water to dispense through the brewer, as a value in ounces.
 • Amount of milk dispensed through the frother, in ounces.
 • Amount of froth to produce; as a value from zero to 100. A value of zero will cause the milk to simply be warmed up (steamed), while a higher value will produce more foam.
 • The amount of coffee to dispense; in kilograms.
 */
public class RequestData{
    public int waterTemp;
    public int waterDisp;
    public int milkDisp;
    public int frothStr;
    public int coffeeDisp;

    public RequestData(){
        this.waterTemp = 70;
        this.waterDisp = 100;
        this.milkDisp = 150;
        this.frothStr = 0;
        this.coffeeDisp = 15;
    }


    public void setWithBasic(CoffeeBrew.BasicState basicState){
        switch (basicState.amount){
            case FIRST:{
                this.waterDisp = 150;
                break;
            }
            case SECOND:{
                this.waterDisp = 275;
                break;
            }
            case THIRD:{
                this.waterDisp = 400;
                break;
            }
        }

        switch (basicState.froth){
            case FIRST:{
                this.frothStr = 0;
                break;
            }
            case SECOND:{
                this.frothStr = 50;
                break;
            }
            case THIRD:{
                this.frothStr = 100;
                break;
            }
        }

        switch (basicState.strength){
            case FIRST:{
                this.coffeeDisp = 15;
                break;
            }
            case SECOND:{
                this.coffeeDisp = 30;
                break;
            }
            case THIRD:{
                this.coffeeDisp = 45;
                break;
            }
        }
    }

    /**
     * Set values with advanced state
     * @param advancedState
     */
    public void setWithAdvance(CoffeeBrew.AdvancedState advancedState){
        this.waterTemp = advancedState.waterState.temp;
        this.waterDisp = advancedState.waterState.disp;
        this.milkDisp = advancedState.milkState.disp;
        this.frothStr = advancedState.frothState.str;
        this.coffeeDisp = advancedState.coffeeState.disp;
    }

    @Override
    public boolean equals(Object o){

        if(o == this) return true;
        if(!(o instanceof RequestData)) return false;

        RequestData r = (RequestData) o;

        if(this.waterDisp == r.waterDisp && this.waterTemp == r.waterTemp &&
                this.milkDisp == r.milkDisp && this.frothStr == r.frothStr &&
                this.coffeeDisp == r.coffeeDisp)
            return true;

        return false;
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("Water Temp: " + waterTemp + "\n");
        builder.append("Water Disp: " + waterDisp + "\n");
        builder.append("Milk Disp: " + milkDisp + "\n");
        builder.append("Froth Disp: " + frothStr + "\n");
        builder.append("Coffee Disp: " + coffeeDisp + "\n");
        return builder.toString();
    }
}
