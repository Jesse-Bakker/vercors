

public final class IntegerChannel {

    private boolean transfering;

    private int exchangeValue;

    /*@
    resource lock_invariant() =
		Perm(transfering, 1)
		** Perm(exchangeValue,1)
		;
    @*/

    public IntegerChannel() {
        transfering = true;
    }

    public synchronized void writeValue(int v) {
        /*@
            loop_invariant Perm(transfering, 1) ** Perm(exchangeValue,1);
		    loop_invariant held(this);
         @*/
        while (!transfering) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
        transfering = false;
        exchangeValue = v;
        notify();
    }

    public synchronized int readValue() {
        /*@
            loop_invariant Perm(transfering, 1) ** Perm(exchangeValue,1);
            loop_invariant held(this);
         */
        while (transfering) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
        transfering = true;
        notify();
        return exchangeValue;
    }
}
