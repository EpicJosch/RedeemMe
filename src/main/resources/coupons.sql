CREATE TABLE IF NOT EXISTS `coupons` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `description` varchar(150) DEFAULT NULL,
  `created` int(12) NOT NULL,
  `creator` varchar(32) DEFAULT NULL,
  `code` varchar(15) DEFAULT NULL,
  `player` varchar(32) DEFAULT NULL,
  `money` double DEFAULT NULL,
  `redeemed` int(12) DEFAULT NULL,
  `embargo` int(12) DEFAULT NULL,
  `expiry` int(12) DEFAULT NULL,
  `server` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;