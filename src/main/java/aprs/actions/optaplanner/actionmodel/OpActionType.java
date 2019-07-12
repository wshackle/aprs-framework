/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.actions.optaplanner.actionmodel;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public enum OpActionType {
    INVALID_OP_ACTION,
    PICKUP,
    DROPOFF,
    FAKE_DROPOFF,
    FAKE_PICKUP,
    START,
    END
}
