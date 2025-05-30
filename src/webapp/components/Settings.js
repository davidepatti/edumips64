import React from 'react';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import Switch from '@mui/material/Switch';
import Button from '@mui/material/Button';

const Settings = ({ viMode, setViMode, fontSize, setFontSize }) => {
    return (

        <Grid container spacing={2} alignItems="center">
            <Grid item>
                <Typography sx={{ fontSize: '0.75rem' }}>Editor Vi Mode</Typography>
            </Grid>
            <Grid item>
                <Switch
                    checked={viMode}
                    onChange={(e) => setViMode(e.target.checked)}
                    color="primary"
                />
            </Grid>
            <Grid item>
                <Typography sx={{ fontSize: '0.75rem' }}>Font Size</Typography>
            </Grid>
            <Grid item>
                <Button variant="contained" onClick={() => setFontSize(fontSize + 1)}>+</Button>
            </Grid>
            <Grid item>
                <Button variant="contained" onClick={() => setFontSize(Math.max(fontSize - 1, 1))}>-</Button>
            </Grid>
        </Grid>
    );
};

export default Settings;