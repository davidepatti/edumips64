import React from 'react';

const StdOut = (props) => {
  return (
    <textarea 
      readOnly 
      value={props.stdout || ''} 
      id="stdout-view"
      style={{
        width: '100%',
        minHeight: '100px',
        fontFamily: 'monospace'
      }}
    />
  );
};

export default StdOut;