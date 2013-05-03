import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import com.vingd.client.VingdClient;
import com.vingd.client.VingdOrder;
import com.vingd.client.VingdPurchase;
import com.vingd.client.exception.VingdOperationException;
import com.vingd.client.exception.VingdTransportException;

public class PurchaseConsoleExample {

	public static void main(String[] args) throws VingdTransportException, VingdOperationException, IOException, NoSuchAlgorithmException {
		VingdClient vingd = new VingdClient("test@vingd.com", VingdClient.SHA1("123"), VingdClient.sandboxEndpointURL, VingdClient.sandboxFrontendURL);

		long oid = vingd.createObject("A test object", "http://localhost:888");
		System.out.println("OID = " + oid);

		VingdOrder order = vingd.createOrder(oid, 200, "ctx", 15*60*1000);
		System.out.println(order);
		System.out.println("Purchase link: "+order.getRedirectURL());

		System.out.println("Input tid: ");
		Scanner input = new Scanner(System.in);
		String tid = input.nextLine();

		VingdPurchase purchase = vingd.verifyPurchase(oid, tid);
		System.out.println("Purchase verified. Buyer HUID = "+purchase.getHUID());

		vingd.commitPurchase(purchase);
		System.out.println("Purchase committed.");
	}

}
