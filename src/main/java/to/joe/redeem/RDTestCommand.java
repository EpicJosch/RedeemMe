package to.joe.redeem;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import to.joe.redeem.exception.CouponCodeAlreadyExistsException;

public class RDTestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            //new PackageBuilder().forPlayer("PlantAssassin").withName("Voting reward").withDescription("Thanks for voting!").withCreator("Senior Staph").withEmbargo(0).withExpiry(1866135254L).withItemStack(new ItemStack(Material.DIAMOND_SWORD)).withItemStack(new ItemStack(Material.BOOK_AND_QUILL)).withCommand("op <player>", true).withCommand("me says hi", false).build();
            //new PackageBuilder().withCode(new CouponCode(2)).withItemStack(new ItemStack(Material.DIAMOND_SWORD)).build();
            //new PackageBuilder().withName("Joke prize").withCode(new CouponCode(-1)).withItemStack(new ItemStack(Material.WOOD_SWORD)).build();
            new CouponCode("ASDF", 10);
            new CouponCode("ASDF", 10);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (CouponCodeAlreadyExistsException e) {
            e.printStackTrace();
        }
        return true;
    }
}