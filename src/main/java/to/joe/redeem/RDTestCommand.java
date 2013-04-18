package to.joe.redeem;

import java.sql.SQLException;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public class RDTestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            //new PackageBuilder().forPlayer("PlantAssassin").withName("Voting reward").withDescription("Thanks for voting!").withCreator("Senior Staph").withEmbargo(0).withExpiry(1866135254L).withItemStack(new ItemStack(Material.DIAMOND_SWORD)).withCommand("op <player>", true).build();
            new PackageBuilder().withCode(new CouponCode(100)).withItemStack(new ItemStack(Material.DIAMOND_SWORD)).build();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}