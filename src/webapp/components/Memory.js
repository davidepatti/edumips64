import React from 'react';

import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';


// Component that shows a binary value.
const BinaryValue = ({hexString, value}) => {
    return (
        <Tooltip disableFocusListener title={value ?? ''}>
            <Typography sx={{
                fontFamily: "Menlo, Monaco, 'Courier New', monospace",
                padding: '0.1em 10px',
                textAlign: 'right',
                fontSize: '0.75rem',
            }}>
                {hexString}
            </Typography>
        </Tooltip>
    );
};


const MemoryElement = ({memoryelement}) => {
    return (
        <>
            <td>
                <BinaryValue hexString={memoryelement.address_hex} value={memoryelement.address}/>
            </td>
            <td>
                <BinaryValue hexString={memoryelement.value_hex} value={memoryelement.value}/>
            </td>
            <td className="elementLabel">{memoryelement.label}</td>
            <td className="elementCode">{memoryelement.code}</td>
            <td className="elementComment">{memoryelement.comment}</td>
        </>
    );
};


const Memory = (props) => {
    const memory = props.memory;
    const {cells = []} = memory;
    return (
        <div>
            {cells.length === 0 ? (
                <Typography>No program loaded</Typography>
            ) : (
                <table id="memory">
                    <thead>
                    <tr>
                        <th style={{textAlign: 'left'}}>Address</th>
                        <th style={{textAlign: 'left'}}>Value</th>
                        <th style={{textAlign: 'left'}}>Label</th>
                        <th style={{textAlign: 'left'}}>Code</th>
                        <th style={{textAlign: 'left'}}>Comment</th>
                    </tr>
                    </thead>
                    <tbody>
                    {cells.map((memoryelement, i) => (
                        <tr key={i}>
                            <MemoryElement memoryelement={cells[i]} />
                        </tr>
                    ))}
                    </tbody>
                </table>
            )}
        </div>
    );
};

export default Memory;
