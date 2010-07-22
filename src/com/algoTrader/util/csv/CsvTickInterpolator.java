package com.algoTrader.util.csv;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

import org.supercsv.exception.SuperCSVException;

import com.algoTrader.entity.Tick;
import com.algoTrader.entity.TickImpl;
import com.algoTrader.util.PropertiesUtil;
import com.algoTrader.util.RoundUtil;

public class CsvTickInterpolator {

    private static String dataSet = PropertiesUtil.getProperty("simulation.dataSet");

    private static double recordsPerDay = 17.0;
     private static double recordsPerHour = 2.0;
     private static double startHour = 9.0;

    public static void main(String[] args) throws SuperCSVException, IOException, ParseException {

        (new File("results/tickdata/" + dataSet + "/" + args[1] + ".csv")).delete();

        CsvTickReader csvReader = new CsvTickReader(args[0]);
        CsvTickWriter csvWriter = new CsvTickWriter(args[1]);

        Tick oldTick = csvReader.readTick();

        Tick newTick;
        while ((newTick = csvReader.readTick()) != null) {

            for (int currentHour=0; currentHour < recordsPerDay; currentHour++) {

                double lastOffset = (newTick.getLast().doubleValue() - oldTick.getLast().doubleValue()) / (recordsPerDay - 1.0);

                Tick tick = new TickImpl();

                tick.setDateTime(new Date(newTick.getDateTime().getTime() + (int)((currentHour / recordsPerHour + startHour) * 60 * 60 * 1000)));
                tick.setLast(RoundUtil.getBigDecimal(oldTick.getLast().doubleValue() + currentHour * lastOffset));
                tick.setLastDateTime(tick.getDateTime());
                tick.setVol(0);
                tick.setVolBid(0);
                tick.setVolAsk(0);
                tick.setBid(new BigDecimal(0));
                tick.setAsk(new BigDecimal(0));
                tick.setOpenIntrest(0);
                tick.setSettlement(new BigDecimal(0));

                csvWriter.write(tick);
            }
            oldTick = newTick;
        }

        csvWriter.close();
    }
}
